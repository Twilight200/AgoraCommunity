package com.example.agora.devtools.datagen;

import com.example.agora.platform.api.IdGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class DataGenService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;
    private final DataGenProperties properties;

    public DataGenService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            IdGenerator idGenerator,
            DataGenProperties properties
    ){
        this.idGenerator = idGenerator;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Transactional
    public void generate() {
        ensureBatchDoesNotExist();

        Random random = new Random(properties.seed());

        List<Long> userIds = generateUsers(random);
        List<Long> categoryIds = generateCategories(random);

        generateTopicsAndReplies(
                random,
                userIds,
                categoryIds
        );

        if (userIds.size() != properties.userCount()) {
            throw new IllegalStateException(
                    "Generated user count does not match configuration."
            );
        }

        if (categoryIds.size() != properties.categoryCount()) {
            throw new IllegalStateException(
                    "Generated category count does not match configuration."
            );
        }
    }


    private void ensureBatchDoesNotExist(){
        Integer userCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from user_account
                where username like ?
                """,
                Integer.class,
                properties.marker() + "%"
        );

        Integer categoryCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from category
                where slug like ?
                """,
                Integer.class,
                properties.marker() + "%"
        );

        if ((userCount != null && userCount > 0)
                || (categoryCount != null && categoryCount > 0)) {
            throw new IllegalStateException(
                    "Datagen batch already exists. marker=" + properties.marker()
            );
        }
    }

    private List<Long> generateUsers(Random random) {
        List<Long> userIds = new ArrayList<>();

        String passwordHash = passwordEncoder.encode("DatagenPassword123!");

        for (int i = 1; i <= properties.userCount(); i++) {
            long userId = idGenerator.nextId();
            String username = properties.marker() + "user_" + i;

            jdbcTemplate.update(
                    """
                    insert into user_account(
                        id,
                        username,
                        password_hash,
                        trust_level,
                        status
                    )
                    values (?, ?, ?, 0, 1)
                    """,
                    userId,
                    username,
                    passwordHash
            );

            jdbcTemplate.update(
                    """
                    insert into user_stat(user_id)
                    values (?)
                    """,
                    userId
            );

            userIds.add(userId);

            random.nextInt();
        }

        return userIds;
    }

    private List<Long> generateCategories(Random random) {
        List<Long> categoryIds = new ArrayList<>();

        for (int i = 1; i <= properties.categoryCount(); i++) {
            long categoryId = idGenerator.nextId();

            String slug = properties.marker() + "category_" + i;
            String name = "Datagen Category " + i;
            int sort = random.nextInt(100);

            jdbcTemplate.update(
                    """
                    insert into category(
                        id,
                        name,
                        slug,
                        sort
                    )
                    values (?, ?, ?, ?)
                    """,
                    categoryId,
                    name,
                    slug,
                    sort
            );

            categoryIds.add(categoryId);
        }

        return categoryIds;
    }

    private List<Long> findGeneratedUserIds() {
        return jdbcTemplate.query(
                """
                select id
                from user_account
                where username like ?
                order by id
                """,
                (resultSet, rowNum) -> resultSet.getLong("id"),
                properties.marker() + "user_%"
        );
    }

    private List<Long> findGeneratedCategoryIds() {
        return jdbcTemplate.query(
                """
                select id
                from category
                where slug like ?
                order by id
                """,
                (resultSet, rowNum) -> resultSet.getLong("id"),
                properties.marker() + "category_%"
        );
    }

    private void generateTopicsAndReplies(
            Random random,
            List<Long> userIds,
            List<Long> categoryIds
    ) {
        for (int topicIndex = 1;
             topicIndex <= properties.topicCount();
             topicIndex++) {

            long topicId = idGenerator.nextId();
            long topicAuthorId = randomUser(random, userIds);
            long categoryId = randomCategory(random, categoryIds);

            int totalFloors = 1 + properties.repliesPerTopic();

            List<Long> replyAuthors = new ArrayList<>();
            replyAuthors.add(topicAuthorId);

            for (int floorNo = 2;
                 floorNo <= totalFloors;
                 floorNo++) {
                replyAuthors.add(randomUser(random, userIds));
            }

            long lastReplyUserId =
                    replyAuthors.get(replyAuthors.size() - 1);

            insertTopic(
                    topicId,
                    categoryId,
                    topicAuthorId,
                    "Datagen Topic " + topicIndex,
                    totalFloors,
                    lastReplyUserId
            );

            for (int index = 0;
                 index < replyAuthors.size();
                 index++) {

                int floorNo = index + 1;
                long replyUserId = replyAuthors.get(index);

                String content;

                if (floorNo == 1) {
                    content = "This is the first post of datagen topic "
                            + topicIndex;
                } else {
                    content = "Datagen reply "
                            + floorNo
                            + " of topic "
                            + topicIndex;
                }

                insertReply(
                        topicId,
                        replyUserId,
                        floorNo,
                        content
                );
            }
        }
    }

    private long randomUser(
            Random random,
            List<Long> userIds
    ) {
        return userIds.get(random.nextInt(userIds.size()));
    }

    private long randomCategory(
            Random random,
            List<Long> categoryIds
    ) {
        return categoryIds.get(random.nextInt(categoryIds.size()));
    }

    private void insertTopic(
            long topicId,
            long categoryId,
            long userId,
            String title,
            int maxFloor,
            long lastReplyUserId
    ) {
        jdbcTemplate.update(
                """
                insert into topic(
                    id,
                    category_id,
                    user_id,
                    title,
                    status,
                    pinned,
                    max_floor,
                    last_reply_user_id,
                    version
                )
                values (?, ?, ?, ?, 'NORMAL', false, ?, ?, 0)
                """,
                topicId,
                categoryId,
                userId,
                title,
                maxFloor,
                lastReplyUserId
        );
    }

    private void insertReply(
            long topicId,
            long userId,
            int floorNo,
            String content
    ) {
        long replyId = idGenerator.nextId();

        jdbcTemplate.update(
                """
                insert into reply(
                    id,
                    topic_id,
                    floor_no,
                    user_id,
                    content,
                    status,
                    version
                )
                values (?, ?, ?, ?, ?, 'NORMAL', 0)
                """,
                replyId,
                topicId,
                floorNo,
                userId,
                content
        );
    }


}

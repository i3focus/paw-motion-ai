package com.pawmotionai.social.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.pawmotionai.social.application.dto.ActivityCommentDto;
import com.pawmotionai.social.application.dto.ActivityPostDto;
import com.pawmotionai.social.application.dto.MapPointsDto;
import com.pawmotionai.social.application.port.out.SocialRepository;

/**
 * Legacy activity-feed endpoints backed by the PETLIFE feed tables (V8). Reactions are stored with their
 * typed value (PAW/HEART/LAUGH) and exposed with their legacy name (LIKE/LOVE/CHEER) until the old clients go away.
 */
@Repository
public class JdbcSocialRepository implements SocialRepository {

    private static final String POST_SELECT = """
        SELECT a.id, a.owner_id, o.display_name AS owner_name, o.avatar_url AS owner_avatar_url,
               a.pet_id, p.name AS pet_name, lower(p.species::text) AS pet_species, lower(a.type::text) AS type,
               a.started_at, a.duration_seconds, a.distance_meters, a.notes, a.latitude, a.longitude, a.created_at
        FROM activities a
        JOIN owners o ON o.id = a.owner_id
        JOIN pets p ON p.id = a.pet_id
        """;

    private static final String LEGACY_TYPE = "CASE r.type WHEN 'PAW' THEN 'LIKE' WHEN 'HEART' THEN 'LOVE' ELSE 'CHEER' END";

    private static final String COMMENT_SELECT = """
        SELECT c.id, f.ref_id AS activity_id, c.owner_id, o.display_name AS owner_name, o.avatar_url AS owner_avatar_url,
               c.body, c.created_at
        FROM comments c
        JOIN feed_items f ON f.id = c.feed_item_id
        JOIN owners o ON o.id = c.owner_id
        """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcSocialRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ActivityPostDto> findRecentPosts(UUID viewerId, int limit, long offset) {
        String sql = POST_SELECT + " WHERE a.share_to_paw_city = TRUE ORDER BY a.started_at DESC, a.id DESC LIMIT ? OFFSET ?";
        List<ActivityPostDto> posts = jdbcTemplate.query(sql, (rs, rowNum) -> toPost(rs), limit, offset);
        return withEngagement(posts, viewerId);
    }

    @Override
    public Optional<ActivityPostDto> findPostById(UUID activityId, UUID viewerId) {
        String sql = POST_SELECT + " WHERE a.id = ?::uuid AND a.share_to_paw_city = TRUE";
        return jdbcTemplate.query(sql, (rs, rowNum) -> toPost(rs), activityId.toString()).stream()
            .findFirst()
            .map(post -> withEngagement(List.of(post), viewerId).get(0));
    }

    @Override
    public Optional<String> findViewerReaction(UUID activityId, UUID ownerId) {
        return jdbcTemplate.query("SELECT " + LEGACY_TYPE + """
                 AS type FROM reactions r JOIN feed_items f ON f.id = r.feed_item_id
                WHERE f.type = 'ACTIVITY' AND f.ref_id = ?::uuid AND r.owner_id = ?::uuid
                """, (rs, rowNum) -> rs.getString("type"), activityId.toString(), ownerId.toString())
            .stream().findFirst();
    }

    @Override
    public void saveReaction(UUID activityId, UUID ownerId, String type) {
        jdbcTemplate.update("""
            INSERT INTO reactions (id, feed_item_id, owner_id, type, legacy_type, created_at, updated_at)
            SELECT ?::uuid, f.id, ?::uuid, CASE ? WHEN 'LIKE' THEN 'PAW' WHEN 'LOVE' THEN 'HEART' ELSE 'LAUGH' END, ?, now(), now()
            FROM feed_items f
            WHERE f.type = 'ACTIVITY' AND f.ref_id = ?::uuid
            ON CONFLICT (feed_item_id, owner_id)
            DO UPDATE SET type = EXCLUDED.type, legacy_type = EXCLUDED.legacy_type, updated_at = now()
            """, UUID.randomUUID().toString(), ownerId.toString(), type, type, activityId.toString());
    }

    @Override
    public void deleteReaction(UUID activityId, UUID ownerId) {
        jdbcTemplate.update("""
            DELETE FROM reactions r USING feed_items f
            WHERE r.feed_item_id = f.id AND f.type = 'ACTIVITY' AND f.ref_id = ?::uuid AND r.owner_id = ?::uuid
            """, activityId.toString(), ownerId.toString());
    }

    @Override
    public List<ActivityCommentDto> findComments(UUID activityId) {
        return jdbcTemplate.query(COMMENT_SELECT + " WHERE f.type = 'ACTIVITY' AND f.ref_id = ?::uuid ORDER BY c.created_at ASC",
            (rs, rowNum) -> toComment(rs), activityId.toString());
    }

    @Override
    public ActivityCommentDto addComment(UUID activityId, UUID ownerId, String body) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO comments (id, feed_item_id, owner_id, body, created_at)
            SELECT ?::uuid, f.id, ?::uuid, ?, now()
            FROM feed_items f
            WHERE f.type = 'ACTIVITY' AND f.ref_id = ?::uuid
            """, id.toString(), ownerId.toString(), body, activityId.toString());
        return jdbcTemplate.query(COMMENT_SELECT + " WHERE c.id = ?::uuid", (rs, rowNum) -> toComment(rs), id.toString())
            .stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Comment was not persisted"));
    }

    @Override
    public MapPointsDto findMapPoints() {
        List<MapPointsDto.PartnerMapPointDto> partners = jdbcTemplate.query("""
            SELECT id, COALESCE(NULLIF(business_name, ''), display_name) AS name, avatar_url, website, latitude, longitude
            FROM owners
            WHERE role = 'PARTNER' AND status = 'ACTIVE' AND latitude IS NOT NULL AND longitude IS NOT NULL
            ORDER BY name
            """, (rs, rowNum) -> new MapPointsDto.PartnerMapPointDto(
                rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("avatar_url"), rs.getString("website"),
                rs.getObject("latitude", Double.class), rs.getObject("longitude", Double.class)));
        List<MapPointsDto.ActivityMapPointDto> activities = jdbcTemplate.query("""
            SELECT a.id, a.pet_id, p.name AS pet_name, lower(a.type::text) AS type, a.latitude, a.longitude, a.started_at
            FROM activities a
            JOIN pets p ON p.id = a.pet_id
            WHERE a.latitude IS NOT NULL AND a.longitude IS NOT NULL AND a.share_to_paw_city = TRUE
            ORDER BY a.started_at DESC
            LIMIT 100
            """, (rs, rowNum) -> new MapPointsDto.ActivityMapPointDto(
                rs.getObject("id", UUID.class), rs.getObject("pet_id", UUID.class), rs.getString("pet_name"),
                rs.getString("type"), rs.getObject("latitude", Double.class), rs.getObject("longitude", Double.class),
                instant(rs, "started_at")));
        return new MapPointsDto(partners, activities);
    }

    @Override
    public boolean activityExists(UUID activityId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM feed_items WHERE type = 'ACTIVITY' AND ref_id = ?::uuid AND deleted_at IS NULL", Long.class,
            activityId.toString());
        return count != null && count > 0;
    }

    @Override
    public Map<String, Long> reactionCounts(UUID activityId) {
        return reactionCountsByActivity(List.of(activityId.toString())).getOrDefault(activityId, Map.of());
    }

    @Override
    public long commentCount(UUID activityId) {
        return commentCountsByActivity(List.of(activityId.toString())).getOrDefault(activityId, 0L);
    }

    private List<ActivityPostDto> withEngagement(List<ActivityPostDto> posts, UUID viewerId) {
        if (posts.isEmpty()) {
            return posts;
        }
        List<String> activityIds = posts.stream().map(post -> post.id().toString()).toList();
        Map<UUID, Map<String, Long>> reactionCounts = reactionCountsByActivity(activityIds);
        Map<UUID, Long> commentCounts = commentCountsByActivity(activityIds);
        Map<UUID, String> viewerReactions = viewerReactionsByActivity(activityIds, viewerId);
        return posts.stream()
            .map(post -> new ActivityPostDto(post.id(), post.ownerId(), post.ownerName(), post.ownerAvatarUrl(), post.petId(),
                post.petName(), post.petSpecies(), post.type(), post.startedAt(), post.durationSeconds(), post.distanceMeters(),
                post.notes(), post.latitude(), post.longitude(), post.createdAt(),
                reactionCounts.getOrDefault(post.id(), Map.of()), commentCounts.getOrDefault(post.id(), 0L),
                viewerReactions.get(post.id())))
            .toList();
    }

    private Map<UUID, Map<String, Long>> reactionCountsByActivity(List<String> activityIds) {
        Map<UUID, Map<String, Long>> counts = new HashMap<>();
        String sql = "SELECT f.ref_id AS activity_id, " + LEGACY_TYPE + " AS type, COUNT(*) AS total "
            + "FROM reactions r JOIN feed_items f ON f.id = r.feed_item_id "
            + "WHERE f.type = 'ACTIVITY' AND f.ref_id IN (" + placeholders(activityIds.size()) + ") GROUP BY f.ref_id, r.type";
        jdbcTemplate.query(sql, rs -> {
            while (rs.next()) {
                counts.computeIfAbsent(rs.getObject("activity_id", UUID.class), ignored -> new HashMap<>())
                    .put(rs.getString("type"), rs.getLong("total"));
            }
            return null;
        }, activityIds.toArray());
        return counts;
    }

    private Map<UUID, Long> commentCountsByActivity(List<String> activityIds) {
        Map<UUID, Long> counts = new HashMap<>();
        String sql = "SELECT f.ref_id AS activity_id, COUNT(*) AS total FROM comments c JOIN feed_items f ON f.id = c.feed_item_id "
            + "WHERE f.type = 'ACTIVITY' AND f.ref_id IN (" + placeholders(activityIds.size()) + ") GROUP BY f.ref_id";
        jdbcTemplate.query(sql, rs -> {
            while (rs.next()) {
                counts.put(rs.getObject("activity_id", UUID.class), rs.getLong("total"));
            }
            return null;
        }, activityIds.toArray());
        return counts;
    }

    private Map<UUID, String> viewerReactionsByActivity(List<String> activityIds, UUID viewerId) {
        Map<UUID, String> reactions = new HashMap<>();
        String sql = "SELECT f.ref_id AS activity_id, " + LEGACY_TYPE + " AS type "
            + "FROM reactions r JOIN feed_items f ON f.id = r.feed_item_id "
            + "WHERE f.type = 'ACTIVITY' AND f.ref_id IN (" + placeholders(activityIds.size()) + ") AND r.owner_id = ?::uuid";
        List<Object> args = new ArrayList<>(activityIds);
        args.add(viewerId.toString());
        jdbcTemplate.query(sql, rs -> {
            while (rs.next()) {
                reactions.put(rs.getObject("activity_id", UUID.class), rs.getString("type"));
            }
            return null;
        }, args.toArray());
        return reactions;
    }

    private String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?::uuid"));
    }

    private ActivityPostDto toPost(ResultSet rs) throws SQLException {
        return new ActivityPostDto(
            rs.getObject("id", UUID.class), rs.getObject("owner_id", UUID.class), rs.getString("owner_name"),
            rs.getString("owner_avatar_url"), rs.getObject("pet_id", UUID.class), rs.getString("pet_name"),
            rs.getString("pet_species"), rs.getString("type"), instant(rs, "started_at"), rs.getLong("duration_seconds"),
            rs.getObject("distance_meters", Double.class), rs.getString("notes"), rs.getObject("latitude", Double.class),
            rs.getObject("longitude", Double.class), instant(rs, "created_at"), Map.of(), 0, null);
    }

    private ActivityCommentDto toComment(ResultSet rs) throws SQLException {
        return new ActivityCommentDto(
            rs.getObject("id", UUID.class), rs.getObject("activity_id", UUID.class), rs.getObject("owner_id", UUID.class),
            rs.getString("owner_name"), rs.getString("owner_avatar_url"), rs.getString("body"), instant(rs, "created_at"));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}

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
        return jdbcTemplate.query(
                "SELECT type FROM activity_reactions WHERE activity_id = ?::uuid AND owner_id = ?::uuid",
                (rs, rowNum) -> rs.getString("type"), activityId.toString(), ownerId.toString())
            .stream().findFirst();
    }

    @Override
    public void saveReaction(UUID activityId, UUID ownerId, String type) {
        jdbcTemplate.update("""
            INSERT INTO activity_reactions (id, activity_id, owner_id, type, created_at, updated_at)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?, now(), now())
            ON CONFLICT (activity_id, owner_id)
            DO UPDATE SET type = EXCLUDED.type, updated_at = now()
            """, UUID.randomUUID().toString(), activityId.toString(), ownerId.toString(), type);
    }

    @Override
    public void deleteReaction(UUID activityId, UUID ownerId) {
        jdbcTemplate.update("DELETE FROM activity_reactions WHERE activity_id = ?::uuid AND owner_id = ?::uuid",
            activityId.toString(), ownerId.toString());
    }

    @Override
    public List<ActivityCommentDto> findComments(UUID activityId) {
        return jdbcTemplate.query("""
            SELECT c.id, c.activity_id, c.owner_id, o.display_name AS owner_name, o.avatar_url AS owner_avatar_url,
                   c.body, c.created_at
            FROM activity_comments c
            JOIN owners o ON o.id = c.owner_id
            WHERE c.activity_id = ?::uuid
            ORDER BY c.created_at ASC
            """, (rs, rowNum) -> toComment(rs), activityId.toString());
    }

    @Override
    public ActivityCommentDto addComment(UUID activityId, UUID ownerId, String body) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO activity_comments (id, activity_id, owner_id, body, created_at)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?, now())
            """, id.toString(), activityId.toString(), ownerId.toString(), body);
        return jdbcTemplate.query("""
            SELECT c.id, c.activity_id, c.owner_id, o.display_name AS owner_name, o.avatar_url AS owner_avatar_url,
                   c.body, c.created_at
            FROM activity_comments c
            JOIN owners o ON o.id = c.owner_id
            WHERE c.id = ?::uuid
            """, (rs, rowNum) -> toComment(rs), id.toString()).stream().findFirst()
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
            "SELECT COUNT(*) FROM activities WHERE id = ?::uuid AND share_to_paw_city = TRUE", Long.class,
            activityId.toString());
        return count != null && count > 0;
    }

    @Override
    public Map<String, Long> reactionCounts(UUID activityId) {
        Map<String, Long> counts = new HashMap<>();
        jdbcTemplate.queryForList(
                "SELECT type, COUNT(*) AS total FROM activity_reactions WHERE activity_id = ?::uuid GROUP BY type",
                activityId.toString())
            .forEach(row -> counts.put((String) row.get("type"), ((Number) row.get("total")).longValue()));
        return counts;
    }

    @Override
    public long commentCount(UUID activityId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM activity_comments WHERE activity_id = ?::uuid", Long.class, activityId.toString());
        return count == null ? 0 : count;
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
        String sql = "SELECT activity_id, type, COUNT(*) AS total FROM activity_reactions WHERE activity_id IN ("
            + placeholders(activityIds.size()) + ") GROUP BY activity_id, type";
        jdbcTemplate.query(sql, rs -> {
            while (rs.next()) {
                UUID activityId = rs.getObject("activity_id", UUID.class);
                counts.computeIfAbsent(activityId, ignored -> new HashMap<>())
                    .put(rs.getString("type"), rs.getLong("total"));
            }
            return null;
        }, activityIds.toArray());
        return counts;
    }

    private Map<UUID, Long> commentCountsByActivity(List<String> activityIds) {
        Map<UUID, Long> counts = new HashMap<>();
        String sql = "SELECT activity_id, COUNT(*) AS total FROM activity_comments WHERE activity_id IN ("
            + placeholders(activityIds.size()) + ") GROUP BY activity_id";
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
        String sql = "SELECT activity_id, type FROM activity_reactions WHERE activity_id IN ("
            + placeholders(activityIds.size()) + ") AND owner_id = ?::uuid";
        List<Object> args = new ArrayList<>();
        args.addAll(activityIds);
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

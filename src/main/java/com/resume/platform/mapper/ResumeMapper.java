package com.resume.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.platform.dto.ResumeSearchVO;
import com.resume.platform.entity.Resume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 简历Mapper
 *
 * 全文搜索实现说明：
 * - 使用 MySQL FULLTEXT 索引（ngram 分词器，支持中文）
 * - 打分规则：姓名精准匹配=100 > 姓名全文40 > 职位30 > 技能20 > 简介10
 * - MATCH ... AGAINST 使用 NATURAL LANGUAGE MODE（分词后按词召回，宽容度高）
 * - LIKE 兜底查询：全文索引无结果时降级，保证不空手而归
 *
 * @author system
 */
@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {

    /**
     * 全文索引搜索简历（加权打分排序）
     *
     * @param keyword 关键词（姓名/职位/技能/简介）
     * @param offset  偏移量
     * @param limit   每页条数
     * @return 搜索结果列表（按相关性降序）
     */
    @Select("SELECT r.id, r.user_id, r.name, r.title, r.avatar, r.skills, r.introduction, "
            + "(CASE WHEN r.name = #{keyword} THEN 100 ELSE 0 END "
            + "+ MATCH(r.name) AGAINST(#{keyword}) * 40 "
            + "+ MATCH(r.title) AGAINST(#{keyword}) * 30 "
            + "+ MATCH(r.skills) AGAINST(#{keyword}) * 20 "
            + "+ MATCH(r.introduction) AGAINST(#{keyword}) * 10) AS relevance, "
            + "(CASE WHEN r.name = #{keyword} THEN 'name' "
            + "WHEN MATCH(r.title) AGAINST(#{keyword}) > 0 THEN 'title' "
            + "WHEN MATCH(r.skills) AGAINST(#{keyword}) > 0 THEN 'skills' "
            + "WHEN MATCH(r.introduction) AGAINST(#{keyword}) > 0 THEN 'introduction' "
            + "ELSE 'name' END) AS matched_field "
            + "FROM resume r "
            + "WHERE r.status = 1 AND ("
            + "r.name = #{keyword} "
            + "OR MATCH(r.name) AGAINST(#{keyword}) > 0 "
            + "OR MATCH(r.title) AGAINST(#{keyword}) > 0 "
            + "OR MATCH(r.skills) AGAINST(#{keyword}) > 0 "
            + "OR MATCH(r.introduction) AGAINST(#{keyword}) > 0) "
            + "ORDER BY relevance DESC, r.update_time DESC "
            + "LIMIT #{offset}, #{limit}")
    List<ResumeSearchVO> searchByFulltext(@Param("keyword") String keyword,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    /**
     * 统计全文索引搜索命中总数
     *
     * @param keyword 关键词
     * @return 命中条数
     */
    @Select("SELECT COUNT(*) FROM resume r "
            + "WHERE r.status = 1 AND ("
            + "r.name = #{keyword} "
            + "OR MATCH(r.name) AGAINST(#{keyword}) > 0 "
            + "OR MATCH(r.title) AGAINST(#{keyword}) > 0 "
            + "OR MATCH(r.skills) AGAINST(#{keyword}) > 0 "
            + "OR MATCH(r.introduction) AGAINST(#{keyword}) > 0)")
    long countByFulltext(@Param("keyword") String keyword);

    /**
     * LIKE 模糊兜底搜索（全文索引无结果时降级使用）
     *
     * @param keyword 关键词
     * @param offset  偏移量
     * @param limit   每页条数
     * @return 搜索结果列表
     */
    @Select("SELECT r.id, r.user_id, r.name, r.title, r.avatar, r.skills, r.introduction, "
            + "(CASE WHEN r.name = #{keyword} THEN 60 "
            + "WHEN r.name LIKE CONCAT('%', #{keyword}, '%') THEN 50 "
            + "WHEN r.title LIKE CONCAT('%', #{keyword}, '%') THEN 30 "
            + "WHEN r.skills LIKE CONCAT('%', #{keyword}, '%') THEN 20 "
            + "ELSE 10 END) AS relevance, "
            + "(CASE WHEN r.name LIKE CONCAT('%', #{keyword}, '%') THEN 'name' "
            + "WHEN r.title LIKE CONCAT('%', #{keyword}, '%') THEN 'title' "
            + "WHEN r.skills LIKE CONCAT('%', #{keyword}, '%') THEN 'skills' "
            + "ELSE 'introduction' END) AS matched_field "
            + "FROM resume r "
            + "WHERE r.status = 1 AND ("
            + "r.name LIKE CONCAT('%', #{keyword}, '%') "
            + "OR r.title LIKE CONCAT('%', #{keyword}, '%') "
            + "OR r.skills LIKE CONCAT('%', #{keyword}, '%') "
            + "OR r.introduction LIKE CONCAT('%', #{keyword}, '%')) "
            + "ORDER BY relevance DESC, r.update_time DESC "
            + "LIMIT #{offset}, #{limit}")
    List<ResumeSearchVO> searchByLike(@Param("keyword") String keyword,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    /**
     * 统计 LIKE 模糊兜底搜索命中总数
     *
     * @param keyword 关键词
     * @return 命中条数
     */
    @Select("SELECT COUNT(*) FROM resume r "
            + "WHERE r.status = 1 AND ("
            + "r.name LIKE CONCAT('%', #{keyword}, '%') "
            + "OR r.title LIKE CONCAT('%', #{keyword}, '%') "
            + "OR r.skills LIKE CONCAT('%', #{keyword}, '%') "
            + "OR r.introduction LIKE CONCAT('%', #{keyword}, '%'))")
    long countByLike(@Param("keyword") String keyword);
}

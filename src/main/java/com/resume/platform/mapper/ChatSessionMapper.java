package com.resume.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.platform.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI对话会话Mapper
 * 遵循阿里巴巴Java开发手册：
 * - Mapper接口使用@Mapper注解
 * - 继承BaseMapper获得基础CRUD能力
 *
 * @author system
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /**
     * 根据会话唯一标识查询会话
     *
     * @param sessionId 会话唯一标识
     * @return 会话实体
     */
    @Select("SELECT * FROM chat_session WHERE session_id = #{sessionId} AND status = 1 LIMIT 1")
    ChatSession selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 分页查询用户的会话列表（按最后消息时间倒序）
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @return 会话分页列表
     */
    @Select("SELECT * FROM chat_session WHERE user_id = #{userId} AND status = 1 ORDER BY last_message_time DESC")
    IPage<ChatSession> selectPageByUserId(Page<ChatSession> page, @Param("userId") Long userId);

    /**
     * 查询用户的所有会话列表（按最后消息时间倒序）
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    @Select("SELECT * FROM chat_session WHERE user_id = #{userId} AND status = 1 ORDER BY last_message_time DESC")
    List<ChatSession> selectListByUserId(@Param("userId") Long userId);
}

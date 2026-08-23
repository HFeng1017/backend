package com.resume.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.platform.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI对话消息Mapper
 * 遵循阿里巴巴Java开发手册：
 * - Mapper接口使用@Mapper注解
 * - 继承BaseMapper获得基础CRUD能力
 *
 * @author system
 */
@Mapper
public interface ChatMessageEntityMapper extends BaseMapper<ChatMessageEntity> {

    /**
     * 查询会话的所有消息（按序号顺序）
     *
     * @param sessionId 会话唯一标识
     * @return 消息列表
     */
    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} AND status = 1 ORDER BY seq_no ASC")
    List<ChatMessageEntity> selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 查询会话的用户消息条数（用于序号递增）
     *
     * @param sessionId 会话唯一标识
     * @return 消息条数
     */
    @Select("SELECT COUNT(*) FROM chat_message WHERE session_id = #{sessionId} AND status = 1")
    int countBySessionId(@Param("sessionId") String sessionId);
}

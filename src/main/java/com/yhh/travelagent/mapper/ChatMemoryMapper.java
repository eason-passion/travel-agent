package com.yhh.travelagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yhh.travelagent.model.ChatMemorys;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


import java.util.List;

/**
 * @Date 2025-07-05 14:22
 * @ClassName: ChatMemoryMapper
 * @Description:
 */
@Mapper
public interface ChatMemoryMapper extends BaseMapper<ChatMemorys> {

    /**
     * 获取最大消息序号
     */
    @Select("SELECT MAX(message_order) FROM chatmemory WHERE conversation_id = #{conversationId} AND is_delete = 0")
    Integer getMaxOrder(@Param("conversationId") String conversationId);

    /**
     * 获取会话消息数量
     */
    @Select("SELECT COUNT(*) FROM chatmemory WHERE conversation_id = #{conversationId} AND is_delete = 0")
    int getMessageCount(@Param("conversationId") String conversationId);

    /**
     * 逻辑删除会话消息
     */
    @Update("UPDATE chatmemory SET is_delete = 1, update_time = NOW() WHERE conversation_id = #{conversationId} AND is_delete = 0")
    int logicalDeleteByConversationId(@Param("conversationId") String conversationId);

    /**
     * 获取最近消息，按消息顺序降序
     */
    @Select("SELECT * FROM chatmemory WHERE conversation_id = #{conversationId} AND is_delete = 0 ORDER BY message_order DESC LIMIT #{limit}")
    List<ChatMemorys> getLatestMessages(@Param("conversationId") String conversationId, @Param("limit") int limit);

    /**
     * 分页获取消息
     */
    @Select("SELECT * FROM chatmemory WHERE conversation_id = #{conversationId} AND is_delete = 0 ORDER BY message_order DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<ChatMemorys> getMessagesPaginated(@Param("conversationId") String conversationId,
                                          @Param("pageSize") int pageSize, @Param("offset") int offset);

    /**
     * 获取指定偏移和数量的消息
     */
    @Select("SELECT * FROM chatmemory WHERE conversation_id = #{conversationId} AND is_delete = 0 ORDER BY message_order DESC LIMIT #{limit} OFFSET #{offset}")
    List<ChatMemorys> getMessagesWithOffset(@Param("conversationId") String conversationId, @Param("limit") int limit,
                                           @Param("offset") int offset);
}

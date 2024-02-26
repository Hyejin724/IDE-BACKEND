package everyide.webide.chatting.message;

import everyide.webide.chatting.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    // 최근 10개 조회
    List<Message> findTop10ByContainerIdOrderBySendDateDesc(String containerId);

    // 해당 메세지의 이전 10개 조회
    List<Message> findTop10ByContainerIdAndIdLessThanOrderBySendDateDesc(String containerId, String id);

    // 해당 메세지의 다음 10개 조회
    List<Message> findTop10ByContainerIdAndIdGreaterThanOrderBySendDateAsc(String containerId, String id);

    // 키워드가 들어간 메세지 조희
    @Query("SELECT m FROM Message m WHERE m.containerId = :containerId AND m.content LIKE %:keyword% ORDER BY m.sendDate DESC")
    List<Message> findByContainerIdAndContentContaining(@Param("containerId") String containerId, @Param("keyword") String keyword);

}

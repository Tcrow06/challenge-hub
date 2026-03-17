package com.challengehub.repository.mongodb;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.challengehub.entity.mongodb.ChatConversationDocument;

public interface ChatConversationRepository extends MongoRepository<ChatConversationDocument, String> {

    Optional<ChatConversationDocument> findByParticipantsHashAndType(String participantsHash, String type);

    Optional<ChatConversationDocument> findByChallengeIdAndChannelKeyAndType(String challengeId, String channelKey,
            String type);

    List<ChatConversationDocument> findByChallengeIdAndTypeAndArchivedFalseOrderByUpdatedAtDesc(String challengeId,
            String type);
}

package com.project.doongdoong.domain.voice.repository;

import com.project.doongdoong.domain.voice.model.Voice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoiceRepository extends JpaRepository<Voice, Long> {
    Optional<Voice> findByAccessUrl(String voiceUrl);
}

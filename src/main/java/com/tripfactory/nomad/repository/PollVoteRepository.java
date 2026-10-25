package com.tripfactory.nomad.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tripfactory.nomad.domain.entity.PollVote;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    @Query("select v from PollVote v join fetch v.option where v.poll.id in :pollIds")
    List<PollVote> findByPollIds(@Param("pollIds") Collection<Long> pollIds);

    @Query("select v from PollVote v where v.poll.id = :pollId and v.user.id = :userId")
    Optional<PollVote> findByPollIdAndUserId(@Param("pollId") Long pollId, @Param("userId") Long userId);
}

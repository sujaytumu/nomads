package com.tripfactory.nomad.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tripfactory.nomad.domain.entity.PollOption;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    @Query("select o from PollOption o where o.poll.id in :pollIds order by o.position asc")
    List<PollOption> findByPollIds(@Param("pollIds") Collection<Long> pollIds);
}

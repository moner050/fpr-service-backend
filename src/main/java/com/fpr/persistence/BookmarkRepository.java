package com.fpr.persistence;

import com.fpr.domain.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByMemberId(Long MemberId);
    void deleteByMemberId(Long memberId);
}

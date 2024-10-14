package com.project.doongdoong.global.repositoty;

import com.project.doongdoong.global.common.RefreshToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

    // accessToken으로 RefreshToken을 찾아온다.
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    Optional<RefreshToken> findByUniqueId(String uniqueId);
}
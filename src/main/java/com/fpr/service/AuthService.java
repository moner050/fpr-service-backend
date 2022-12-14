package com.fpr.service;

import com.fpr.domain.Member;
import com.fpr.domain.Product;
import com.fpr.dto.LoginDto;
import com.fpr.dto.MemberRequestDto;
import com.fpr.dto.MemberResponseDto;
import com.fpr.dto.TokenDto;
import com.fpr.jwt.TokenProvider;
import com.fpr.persistence.MemberRepository;
import com.fpr.persistence.ProductRepository;
import com.fpr.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.fpr.jwt.TokenProvider.REFRESH_TOKEN_EXPIRE_TIME;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RedisUtil redisUtil;

    @Transactional
    public TokenDto signup(MemberRequestDto memberRequestDto) {
        if (memberRepository.existsByEmail(memberRequestDto.getEmail())) {
            throw new RuntimeException("이미 가입되어 있는 유저입니다");
        }

        if(!memberRequestDto.getPassword().equals(memberRequestDto.getConfirmPassword())){
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        Member member = memberRequestDto.toMember(passwordEncoder);
        if(member.getAge() > 0 & member.getAge() < 30){
            List<Product> productList = productRepository.findByJoinWay("스마트폰");
            for (int i = 0; i < productList.size(); i++) {
                member.addProduct(productList.get(i));
            }
        }
        else if(member.getAge() >= 30 & member.getAge() < 40) {
            List<Product> productList = productRepository.findByJoinWayContaining("인터넷");
            for (int i = 0; i < productList.size(); i++) {
                member.addProduct(productList.get(i));
            }
        }
        else if(member.getAge() >= 40 & member.getAge() < 50) {
            List<Product> productList = productRepository.findByJoinWayContaining("전화");
            for (int i = 0; i < productList.size(); i++) {
                member.addProduct(productList.get(i));
            }
        }
        else{
            List<Product> productList = productRepository.findByJoinWayContaining("영업점");
            for (int i = 0; i < productList.size(); i++) {
                member.addProduct(productList.get(i));
            }
        }
        MemberResponseDto.of(memberRepository.save(member));

        // 1. Login ID/PW 를 기반으로 AuthenticationToken 생성
        UsernamePasswordAuthenticationToken authenticationToken = memberRequestDto.toAuthentication();

        // 2. 실제로 검증 (사용자 비밀번호 체크) 이 이루어지는 부분
        //    authenticate 메서드가 실행이 될 때 CustomUserDetailsService 에서 만들었던 loadUserByUsername 메서드가 실행됨
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

        // 4. RefreshToken 저장
        redisUtil.setDataExpire(authentication.getName(), tokenDto.getRefreshToken(), REFRESH_TOKEN_EXPIRE_TIME);

        // 5. 토큰 발급
        return tokenDto;
    }

    @Transactional
    public TokenDto login(LoginDto loginDto) {
        // 1. Login ID/PW 를 기반으로 AuthenticationToken 생성
        UsernamePasswordAuthenticationToken authenticationToken = loginDto.toAuthentication();

        // 2. 실제로 검증 (사용자 비밀번호 체크) 이 이루어지는 부분
        //    authenticate 메서드가 실행이 될 때 CustomUserDetailsService 에서 만들었던 loadUserByUsername 메서드가 실행됨
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

        // 4. RefreshToken 저장
        redisUtil.setDataExpire(authentication.getName(), tokenDto.getRefreshToken(), REFRESH_TOKEN_EXPIRE_TIME);

        // 5. 토큰 발급
        return tokenDto;
    }

}
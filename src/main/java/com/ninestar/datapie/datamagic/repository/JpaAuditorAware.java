package com.ninestar.datapie.datamagic.repository;

import cn.hutool.core.util.StrUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
public class JpaAuditorAware implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || auth.getCredentials()==null){
            // there is no loginUser when register
            return Optional.empty();
        }

        String loginUser = auth.getCredentials().toString();
        if(StrUtil.isEmpty(loginUser)){
            return Optional.empty();
        }
        else{
            return Optional.of(loginUser);
        }
    }
}

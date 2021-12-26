package org.sunrise.jmx.server.config;

import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
//@EnableWebSecurity
public class SecurityConfig /*extends WebSecurityConfigurerAdapter*/ {
//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        String passwd = passwordEncoder().encode(JmxConstants.getPassword());
//        auth.inMemoryAuthentication()
//                .withUser(JmxConstants.getUser()).roles("agent").password(passwd);
//    }
//
//    @Bean
//    PasswordEncoder passwordEncoder() {
////        return NoOpPasswordEncoder.getInstance();
//        return new BCryptPasswordEncoder();
//    }
//
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http.authorizeRequests().anyRequest().permitAll();
////        http
////                .authorizeRequests()
////                .antMatchers("/upload/metrics").hasRole("agent")
////                .anyRequest().authenticated()
////                .and()
////                .httpBasic().and()
////                .csrf().ignoringAntMatchers("/upload/metrics");
//
//    }
//
//    @Override
//    public void configure(WebSecurity web) throws Exception {
//        web.ignoring().antMatchers("/actuator/health", "/health", "/metrics", "/metrics/**");
//    }
//
}


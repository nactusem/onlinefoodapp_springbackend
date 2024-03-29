package com.bilgeadam.onlinefoodapp.jwt.resource;

import com.bilgeadam.onlinefoodapp.jwt.JwtTokenUtil;
import com.bilgeadam.onlinefoodapp.jwt.JwtUserDetails;
import com.bilgeadam.onlinefoodapp.jwt.JwtUserDetailsAdminService;
import com.bilgeadam.onlinefoodapp.jwt.JwtUserDetailsCustomerService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/admin")
public class JwtAuthenticationAdminController {

    private final AuthenticationManager authenticationManagerAdmin;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtUserDetailsAdminService jwtUserDetailsAdminService;

    public JwtAuthenticationAdminController(@Qualifier("authenticationManagerAdmin") AuthenticationManager authenticationManagerAdmin,
                                            JwtTokenUtil jwtTokenUtil,
                                            JwtUserDetailsAdminService jwtUserDetailsAdminService,
                                            JwtUserDetailsCustomerService jwtUserDetailsCustomerService) {
        this.authenticationManagerAdmin = authenticationManagerAdmin;
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtUserDetailsAdminService = jwtUserDetailsAdminService;
    }

    @RequestMapping(value = "/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtTokenRequest authenticationRequest)
            throws AuthenticationException {

        authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());

        final UserDetails userDetails = jwtUserDetailsAdminService.loadUserByUsername(authenticationRequest.getUsername());

        final String token = jwtTokenUtil.generateToken(userDetails, "admin");

        return ResponseEntity.ok(new JwtTokenResponse(token));
    }

    @RequestMapping(value = "/refresh", method = RequestMethod.GET)
    public ResponseEntity<?> refreshAndGetAuthenticationToken(HttpServletRequest request) {
        String authToken = request.getHeader("Authorization");
        final String token = authToken.substring(7);
        String username = jwtTokenUtil.getUsernameFromToken(token);
        JwtUserDetails user = (JwtUserDetails) jwtUserDetailsAdminService.loadUserByUsername(username);

        if (jwtTokenUtil.canTokenBeRefreshed(token)) {
            String refreshedToken = jwtTokenUtil.refreshToken(token);
            return ResponseEntity.ok(new JwtTokenResponse(refreshedToken));
        } else {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @ExceptionHandler({AuthenticationException.class})
    public ResponseEntity<String> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

    private void authenticate(String username, String password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);

        try {
            authenticationManagerAdmin.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            throw new AuthenticationException("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new AuthenticationException("INVALID_CREDENTIALS", e);
        }
    }
}
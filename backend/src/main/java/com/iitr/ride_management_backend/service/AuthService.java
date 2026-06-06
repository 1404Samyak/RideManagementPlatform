package com.iitr.ride_management_backend.service;

import com.iitr.ride_management_backend.domain.DriverProfile;
import com.iitr.ride_management_backend.domain.PassengerProfile;
import com.iitr.ride_management_backend.domain.Role;
import com.iitr.ride_management_backend.domain.User;
import com.iitr.ride_management_backend.domain.Vehicle;
import com.iitr.ride_management_backend.dto.AuthResponse;
import com.iitr.ride_management_backend.dto.LoginRequest;
import com.iitr.ride_management_backend.dto.RegisterDriverRequest;
import com.iitr.ride_management_backend.dto.RegisterPassengerRequest;
import com.iitr.ride_management_backend.dto.UpdateProfileRequest;
import com.iitr.ride_management_backend.dto.UserResponse;
import com.iitr.ride_management_backend.exception.BadRequestException;
import com.iitr.ride_management_backend.repository.DriverProfileRepository;
import com.iitr.ride_management_backend.repository.PassengerProfileRepository;
import com.iitr.ride_management_backend.repository.UserRepository;
import com.iitr.ride_management_backend.repository.VehicleRepository;
import com.iitr.ride_management_backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PassengerProfileRepository passengerProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ResponseMapper mapper;

    public AuthService(
            UserRepository userRepository,
            PassengerProfileRepository passengerProfileRepository,
            DriverProfileRepository driverProfileRepository,
            VehicleRepository vehicleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            ResponseMapper mapper
    ) {
        this.userRepository = userRepository;
        this.passengerProfileRepository = passengerProfileRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.mapper = mapper;
    }

    @Transactional
    public AuthResponse registerPassenger(RegisterPassengerRequest request) {
        ensureEmailIsAvailable(request.email());
        User user = new User(
                request.name().trim(),
                normalizeEmail(request.email()),
                request.phone().trim(),
                passwordEncoder.encode(request.password()),
                Role.PASSENGER
        );
        User savedUser = userRepository.save(user);

        PassengerProfile profile = new PassengerProfile(savedUser);
        profile.setCampusAddress(blankToNull(request.campusAddress()));
        passengerProfileRepository.save(profile);

        return authResponse(savedUser);
    }

    @Transactional
    public AuthResponse registerDriver(RegisterDriverRequest request) {
        ensureEmailIsAvailable(request.email());
        User user = new User(
                request.name().trim(),
                normalizeEmail(request.email()),
                request.phone().trim(),
                passwordEncoder.encode(request.password()),
                Role.DRIVER
        );
        User savedUser = userRepository.save(user);

        DriverProfile profile = new DriverProfile(
                savedUser,
                request.licenseNumber().trim(),
                request.verificationDocument().trim()
        );
        driverProfileRepository.save(profile);

        Vehicle vehicle = new Vehicle(
                savedUser,
                request.vehicleNumber().trim(),
                request.vehicleType().trim(),
                request.vehicleCapacity()
        );
        vehicleRepository.save(vehicle);

        return authResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                normalizeEmail(request.email()),
                request.password()
        ));
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(User user) {
        return mapper.userResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(User user, UpdateProfileRequest request) {
        user.setName(request.name().trim());
        user.setPhone(request.phone().trim());
        if (user.getRole() == Role.PASSENGER) {
            passengerProfileRepository.findByUserId(user.getId()).ifPresent(profile -> {
                profile.setCampusAddress(blankToNull(request.campusAddress()));
                passengerProfileRepository.save(profile);
            });
        }
        return mapper.userResponse(userRepository.save(user));
    }

    private AuthResponse authResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), mapper.userResponse(user));
    }

    private void ensureEmailIsAvailable(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

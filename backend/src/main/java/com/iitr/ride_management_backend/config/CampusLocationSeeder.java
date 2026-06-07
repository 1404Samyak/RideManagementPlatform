package com.iitr.ride_management_backend.config;

import com.iitr.ride_management_backend.domain.CampusLocation;
import com.iitr.ride_management_backend.repository.CampusLocationRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CampusLocationSeeder {

    @Bean
    CommandLineRunner seedCampusLocations(CampusLocationRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.saveAll(List.of(
                    new CampusLocation("Main Gate", "Gate", 29.86509, 77.89155),
                    new CampusLocation("Century Gate", "Gate", 29.87037, 77.89949),
                    new CampusLocation("Main Building", "Academic", 29.86484, 77.89639),
                    new CampusLocation("Central Library", "Academic", 29.86417, 77.89783),
                    new CampusLocation("Student Activity Centre", "Campus Life", 29.86572, 77.89922),
                    new CampusLocation("Convocation Hall", "Academic", 29.86382, 77.89869),
                    new CampusLocation("Hospital", "Health", 29.86881, 77.90112),
                    new CampusLocation("LBS Stadium", "Sports", 29.86856, 77.89982),
                    new CampusLocation("Rajiv Bhawan", "Hostel", 29.86998, 77.89421),
                    new CampusLocation("Cautley Bhawan", "Hostel", 29.86633, 77.89217),
                    new CampusLocation("Ravindra Bhawan", "Hostel", 29.86931, 77.89294),
                    new CampusLocation("Jawahar Bhawan", "Hostel", 29.86365, 77.89365)
            ));
        };
    }
}

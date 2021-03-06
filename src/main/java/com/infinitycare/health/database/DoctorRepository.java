package com.infinitycare.health.database;

import com.infinitycare.health.login.model.DoctorDetails;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends MongoRepository<DoctorDetails, String> {

    @Query("{mUserName: { $regex: ?0, $options: 'i' }})")
    List<DoctorDetails> findDoctorsWithSimilarUserName(String doctorName);

    @Query("{'mFirstName': {$regex : ?0, $options: 'i'}}")
    List<DoctorDetails> findDoctorsWithSimilarFirstName(String firstName);

    @Query("{mLastName: { $regex: ?0, $options: 'i'}})")
    List<DoctorDetails> findDoctorsWithSimilarLastName(String lastName);

    @Query("{mSpecialization: { $regex: ?0, $options: 'i'}})")
    List<DoctorDetails> findDoctorsWithSimilarSpecializations(String specialization);

    @Query("{mAddress: { $regex: ?0, $options: 'i'}})")
    List<DoctorDetails> findDoctorsWithSimilarLocations(String specialization);
}

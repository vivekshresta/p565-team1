package com.infinitycare.health.login.service;

import com.google.api.client.json.JsonParser;
import com.infinitycare.health.database.DoctorRepository;
import com.infinitycare.health.database.IpRepository;
import com.infinitycare.health.database.PatientRepository;
import com.infinitycare.health.login.SendEmailSMTP;
import com.infinitycare.health.login.model.CookieDetails;
import com.infinitycare.health.login.model.DoctorDetails;
import com.infinitycare.health.login.model.IPDetails;
import com.infinitycare.health.login.model.PatientDetails;
import com.infinitycare.health.security.TextSecurer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LoginService extends CookieDetails {

    @Autowired
    public PatientRepository patientRepository;

    @Autowired
    public DoctorRepository doctorRepository;

    @Autowired
    public IpRepository ipRepository;

    public LoginService(PatientRepository patientRepository, DoctorRepository doctorRepository, IpRepository ipRepository){
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.ipRepository = ipRepository;
    }

    public ResponseEntity<?> validateCredentials(HttpServletRequest request, HttpServletResponse response, String userType) {
        boolean isCredentialsAccurate = false;
        boolean sentOtp = false;

        String otp = SendEmailSMTP.generateRandomNumber(1000, 9999);
        Map<String, Object> result = new HashMap<>();
        try {
            System.out.println(request.getReader().lines().collect(Collectors.joining(System.lineSeparator())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String username = request.getParameter(USERNAME);
        String password = TextSecurer.encrypt(request.getParameter(PASSWORD));

//        String username = "";
//        String password = "";
//        for(String param : request.getParameterMap().keySet()) {
//            try {
//                JSONObject userDetails = (JSONObject) new JSONParser().parse(param);
//                username = userDetails.get(USERNAME).toString();
//                password = userDetails.get(PASSWORD).toString();
//            } catch (ParseException e) {
//                e.printStackTrace();
//            }
//        }
//          password = TextSecurer.encrypt(password);

        if(userType.equals(PATIENT)) {
            PatientDetails patientDetails = new PatientDetails(username, password);
            if(checkIfPatientCredentialsAreAccurate(patientDetails)){
                isCredentialsAccurate = true;
                patientDetails.setMFAToken(otp);
                patientRepository.save(patientDetails);
            }
        }

        if(userType.equals(DOCTOR)) {
            DoctorDetails doctorDetails = new DoctorDetails(username, password);
            if(checkIfDoctorCredentialsAreAccurate(doctorDetails)) {
                isCredentialsAccurate = true;
                doctorDetails.setMFAToken(otp);
                doctorRepository.save(doctorDetails);
            }
        }

        if(userType.equals(INSURANCE_PROVIDER)) {
            IPDetails ipDetails = new IPDetails(username, password);
            if(checkIfIpCredentialsAreAccurate(ipDetails)) {
                isCredentialsAccurate = true;
                ipDetails.setMFAToken(otp);
                ipRepository.save(ipDetails);
            }
        }

        if(isCredentialsAccurate) {
            SendEmailSMTP.sendFromGMail(new String[]{username}, "Please enter the OTP in the login screen", otp);
            sentOtp = true;
        }

        result.put(IS_CREDENTIALS_ACCURATE, isCredentialsAccurate);
        result.put(IS_OTP_SENT, sentOtp);

        SetEncryptedSessionId.setEncryptedSessionId(request, response, username, userType);
        return ResponseEntity.ok(result);
    }

    private boolean checkIfPatientCredentialsAreAccurate(PatientDetails patientDetails) {
        String enteredUsername = patientDetails.getUserName();
        String enteredPassword = patientDetails.getPassword();

        Optional<PatientDetails> userQueriedFromDB = patientRepository.findById(Integer.toString(enteredUsername.hashCode()));

        // user not found in database
        return userQueriedFromDB.map(details -> details.getPassword().equals(enteredPassword)).orElse(false);
    }

    private boolean checkIfDoctorCredentialsAreAccurate(DoctorDetails doctorDetails) {
        String enteredUsername = doctorDetails.getUserName();
        String enteredPassword = doctorDetails.getPassword();

        Optional<DoctorDetails> userQueriedFromDB = doctorRepository.findById(Integer.toString(enteredUsername.hashCode()));

        // user not found in database
        return userQueriedFromDB.map(details -> details.getPassword().equals(enteredPassword)).orElse(false);
    }

    private boolean checkIfIpCredentialsAreAccurate(IPDetails ipDetails) {
        String enteredUsername = ipDetails.getUserName();
        String enteredPassword = ipDetails.getPassword();

        Optional<IPDetails> userQueriedFromDB = ipRepository.findById(Integer.toString(enteredUsername.hashCode()));
        // user not found in database
        return userQueriedFromDB.map(details -> details.getPassword().equals(enteredPassword)).orElse(false);
    }
}

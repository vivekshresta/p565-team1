package com.infinitycare.health.login.service;

import com.infinitycare.health.billing.BillingService;
import com.infinitycare.health.database.AppointmentsRepository;
import com.infinitycare.health.database.DoctorRepository;
import com.infinitycare.health.database.IpPlanRepository;
import com.infinitycare.health.database.PatientRepository;
import com.infinitycare.health.login.SendEmailSMTP;
import com.infinitycare.health.login.model.*;
import com.mongodb.BasicDBObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class AppointmentsService extends ServiceUtility {

    @Autowired
    public PatientRepository patientRepository;

    @Autowired
    public DoctorRepository doctorRepository;
    
    @Autowired
    public IpPlanRepository ipPlanRepository;

    @Autowired
    public AppointmentsRepository appointmentsRepository;

    @Autowired
    private BillingService billingService;

    public AppointmentsService(PatientRepository patientRepository, DoctorRepository doctorRepository, AppointmentsRepository appointmentsRepository,
                               IpPlanRepository ipPlanRepository, BillingService billingService) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentsRepository = appointmentsRepository;
        this.ipPlanRepository = ipPlanRepository;
        this.billingService = billingService;
    }

    public ResponseEntity<?> getTimeSlots(HttpServletRequest request, String doctorusername) {
        Map<String, String> postBody = getPostBodyInAMap(request);
        String date = getProperParsedDate(postBody.get("date")); // Date format: mm/dd/yyyy

        // 0:9, 1:10, 2:11, 3:12, 4:1, 5:2, 6:3, 7:4.
        List<Integer> availableTimeSlots = new ArrayList<>();
        Collections.addAll(availableTimeSlots, 0, 1, 2, 3, 4, 5, 6, 7);

        Map<String, Object> result = new HashMap<>();
        List<Integer> timeSlotIds = new ArrayList<>();
        List<String> finalts = new ArrayList<>();

        Optional<DoctorDetails> doctorQueriedFromDB = doctorRepository.findById(Integer.toString(doctorusername.hashCode()));

        if(doctorQueriedFromDB.isPresent()) {
            ArrayList timeSlots = doctorQueriedFromDB.get().mTimeSlots;
            for (Object timeSlot : timeSlots) {
                BasicDBObject ts = new BasicDBObject((LinkedHashMap) timeSlot);
                if (ts.get("date").equals(date)) {
                    timeSlotIds = (List<Integer>) ts.get("ts");
                    break;
                }
            }

            if (timeSlotIds == null && timeSlotIds.isEmpty()) { timeSlotIds = availableTimeSlots; }
            else {
                for (Integer availableTimeSlot : availableTimeSlots) {
                    if (!timeSlotIds.contains(availableTimeSlot)) { finalts.add(get12HrTime(availableTimeSlot)); }
                }
            }
        }

        result.put("TimeSlots", finalts);
        return ResponseEntity.ok(result);
    }

    private String get12HrTime(Integer availableTimeSlot) {
        if(availableTimeSlot > 3) {
            return (availableTimeSlot - 3) + ":00 PM";
        } else if(availableTimeSlot == 3) {
            return "12:00 PM";
        }
        return (9 + availableTimeSlot) + ":00 AM";
    }

    private String getProperParsedDate(String date) {
        String[] arr = date.split("/");
        StringBuilder newDate = new StringBuilder();
        for(int i = 0; i < arr.length; i++) {
            newDate.append(arr[i].length() == 1 ? "0" + arr[i] : arr[i]).append("/");
        }

        return newDate.substring(0, newDate.length() - 1);
    }

    public ResponseEntity<?> createAppointments(HttpServletRequest request) {
        String username = getUsername(request);
        Map<String, String> postBody = getPostBodyInAMap(request);
        String doctorusername = new String(Base64.getDecoder().decode(postBody.get("doctorusername")));
        String time = postBody.get("time");
        String datestring = getProperParsedDate(postBody.get("date"));

        Date date = new Date();
        boolean isAppointmentCreated = false;
        AppointmentsDetails appointmentsDetails = null;
        Map<String, Object> result = new HashMap<>();

        DateFormat inFormat = new SimpleDateFormat( "MM/dd/yyyy");

        try { date = inFormat.parse(datestring); }
        catch ( ParseException e ) { e.printStackTrace(); }

        date.setHours(getTimeSlotToStoreInDB(time, ":") + 9);

        Optional<DoctorDetails> doctorQueriedFromDB = doctorRepository.findById(Integer.toString(doctorusername.hashCode()));
        Optional<PatientDetails> patientQueriedFromDB = patientRepository.findById(Integer.toString(username.hashCode()));

        if(doctorQueriedFromDB.isPresent() && patientQueriedFromDB.isPresent()) {
            appointmentsDetails = new AppointmentsDetails(username, doctorusername, date, doctorQueriedFromDB.get().mHospital,
                    doctorQueriedFromDB.get().mAddress, (patientQueriedFromDB.get().mFirstName + " " + patientQueriedFromDB.get().mLastName),
                    (doctorQueriedFromDB.get().mFirstName + " " + doctorQueriedFromDB.get().mLastName));
            appointmentsDetails.setReason(postBody.get("reason"));
            appointmentsDetails.setInsurancePlan(patientQueriedFromDB.get().getInsurancePlan());
            appointmentsRepository.save(appointmentsDetails);
            isAppointmentCreated = true;

            DoctorDetails doctorDetails = doctorQueriedFromDB.get();
            ArrayList timeSlots = doctorQueriedFromDB.get().mTimeSlots;
            List<Integer> timeSlotIds = new ArrayList<>();
            BasicDBObject newTimeSlot = new BasicDBObject();
            boolean isDateFound = false;

            for (Object timeSlot : timeSlots) {
                BasicDBObject ts = new BasicDBObject((LinkedHashMap) timeSlot);
                if (ts.get("date").equals(datestring)) {
                    newTimeSlot = ts;
                    timeSlotIds = (List<Integer>) ts.get("ts");
                    timeSlotIds.add(date.getHours() - 9);
                    ((LinkedHashMap) timeSlot).replace("ts", timeSlotIds);
                    isDateFound = true;
                    break;
                }
            }

            if(!isDateFound) {
                newTimeSlot.put("date", datestring);
                timeSlotIds.add(date.getHours() - 9);
                newTimeSlot.put("ts", timeSlotIds);
                timeSlots.add(newTimeSlot);
            }

            doctorDetails.setTimeSlots(timeSlots);
            doctorRepository.save(doctorDetails);
        }

        String emailBody = "<h1>" + "InfinityCare" + "</h1>\n\n" +"<h2>" + "Your Appointment is Confirmed with "
                + appointmentsDetails.mDoctorName + "</h2>\n" + "<h3>" + "When: " + appointmentsDetails.mDate + "</h3>\n"
                + "<h3>" + "Where: " + appointmentsDetails.mHospital + " "+ appointmentsDetails.mLocation + "</h3>";

        SendEmailSMTP.sendFromGMail(new String[]{username, doctorusername}, "Appointment Confirmed", emailBody);

        result.put("isAppointmentCreated", isAppointmentCreated);
        return ResponseEntity.ok(result);
    }

    private Integer getTimeSlotToStoreInDB(String aTime, String splitParameter) {
        boolean isAM = aTime.substring(aTime.length() - 2).equalsIgnoreCase("AM");
        int time = Integer.valueOf(aTime.split(splitParameter)[0]);
        if(isAM) {
            return time - 9;
        }

        if(time == 12) {
            return 3;
        }

        return time + 3;
    }

    public ResponseEntity<?> getAppointments(HttpServletRequest request, String userType) {
        // doing a sanity check when sending appointments to the user.
        Map<String, Object> results = new HashMap();
        String username = getUsername(request);
        //String username = request.getParameter(USERNAME);

        List<AppointmentsDetails> appointmentsList = null;
        Date now = new Date();

        if(userType.equals(PATIENT)) {
            appointmentsList = appointmentsRepository.findAllPatientAppointments(username);
            for(int i = 0; i < appointmentsList.size(); i++) {
                AppointmentsDetails appointment = appointmentsList.get(i);
                if(now.compareTo(appointment.mDate) > 0) {
                    appointment.setStatus(false);
                    appointmentsRepository.save(appointment);
                    appointmentsList.remove(appointment);
                    i--;
                }
            }

            billingService.getPatientsUnpaidBills(results, username);
        }

        if(userType.equals(DOCTOR)) {
            appointmentsList = appointmentsRepository.findAllDoctorAppointments(username);
            for(int i = 0; i < appointmentsList.size(); i++) {
                AppointmentsDetails appointment = appointmentsList.get(i);
                if(now.compareTo(appointment.mDate) > 0) {
                    appointment.setStatus(false);
                    appointmentsRepository.save(appointment);
                    appointmentsList.remove(appointment);
                    i--;
                }
            }
        }

        for(AppointmentsDetails appointment : appointmentsList) {
            // Needs to be handled seperately and can't be merged with the above 'for loops'.
            // This is because, if a list size is updated, we get ConcurrentModifiedException.
            // And only in this type of for loop, can we update an object in the list and it stays that way
            handleErroneousData(appointment);
        }

        results.put("CurrentAppointments", appointmentsList);
        List<AppointmentsDetails> pastAppointments = getPastAppointmentsAsList(request, userType);

        for(AppointmentsDetails appointment: appointmentsList) {
            appointment.setDisplayTime(get12HrTime(Integer.valueOf(appointment.getDisplayTime())));
        }
        for(AppointmentsDetails appointment: pastAppointments) {
            appointment.setDisplayTime(get12HrTime(Integer.valueOf(appointment.getDisplayTime())));
        }
        if (pastAppointments.isEmpty()) {
            results.put("PastAppointments", new ArrayList<AppointmentsDetails>());
        } else {
            results.put("PastAppointments", pastAppointments);
        }

        return ResponseEntity.ok(results);
    }

    private void handleErroneousData(AppointmentsDetails appointment) {
        boolean isChanged = false;
        if(appointment.getDisplayTime().contains("M")) {
            //If it contains AM or PM in the 'about to be displayed time'
            appointment.setDisplayTime(String.valueOf(getTimeSlotToStoreInDB(appointment.getDisplayTime(), " ")));
            isChanged = true;
        }

        if(Integer.parseInt(appointment.getDisplayTime()) > 7) {
            appointment.setDisplayTime(String.valueOf(Integer.parseInt(appointment.getDisplayTime()) - 9));
            isChanged = true;
        }

        if(appointment.getDisplayDate().contains("/")) {
            appointment.formatDisplayDate(appointment.getDate());
            isChanged = true;
        }

        if(isChanged) {
            appointmentsRepository.save(appointment);
        }
    }

    public ResponseEntity<?> getPastAppointments(HttpServletRequest request, String userType) {
        Map<String, Object> result = new HashMap();

        result.put("Appointments", getPastAppointmentsAsList(request, userType));
        return ResponseEntity.ok(result);
    }

    private List<AppointmentsDetails> getPastAppointmentsAsList(HttpServletRequest request, String userType) {
        String username = getUsername(request);

        List<AppointmentsDetails> result = new ArrayList();

        if(userType.equals(PATIENT)) {
            List<AppointmentsDetails> appointmentsList = appointmentsRepository.findAllPatientAppointments(username);
            for (AppointmentsDetails appointmentsDetails : appointmentsList) {
                if (!appointmentsDetails.getStatus()) {
                    handleErroneousData(appointmentsDetails);
                    result.add(appointmentsDetails);
                }
            }
        }

        if(userType.equals(DOCTOR)) {
            List<AppointmentsDetails> appointmentsList = appointmentsRepository.findAllDoctorAppointments(username);
            for (AppointmentsDetails appointmentsDetails : appointmentsList) {
                if (!appointmentsDetails.getStatus()) {
                    handleErroneousData(appointmentsDetails);
                    result.add(appointmentsDetails);
                }
            }
        }

        return result;
    }

    public ResponseEntity<?> cancelAppointments(HttpServletRequest request, String userType) {
        Map<String, String> postBody = getPostBodyInAMap(request);
        String id = postBody.get("id");

        boolean isAppointmentDeleted = false;
        Map<String, Object> result = new HashMap<>();

        if(userType.equals(PATIENT) || userType.equals(DOCTOR)) {
            Optional<AppointmentsDetails> appt = appointmentsRepository.findById(id);
            if(appt.isPresent()) {
                DateFormat dateFormatter = new SimpleDateFormat( "MM/dd/yyyy");
                String datestring = dateFormatter.format(appt.get().getDate());
                Integer timeSlot = Integer.parseInt(appt.get().getDisplayTime());

                updateDeletedTimeSlotForDoctor(appt.get().getDoctorUsername(), datestring, timeSlot);
                appointmentsRepository.deleteById(id);
                isAppointmentDeleted = true;

                String emailBody = "<h1>" + "InfinityCare" + "</h1>\n\n" +"<h2>" + "Your Appointment with "
                        + appt.get().mDoctorName + "has been cancelled." + "</h2>\n" + "<h3>" + "When: " + appt.get().mDate + "</h3>\n"
                        + "<h3>" + "Where: " + appt.get().mHospital + " "+ appt.get().mLocation + "</h3>";

                SendEmailSMTP.sendFromGMail(new String[]{appt.get().mDoctorUsername, appt.get().mPatientUsername}, "Appointment Cancelled", emailBody);
            }
        }

        result.put("isAppointmentDeleted", isAppointmentDeleted);
        return ResponseEntity.ok(result);
    }

    private void updateDeletedTimeSlotForDoctor(String doctorUserName, String datestring, Integer timeSlot) {
        Optional<DoctorDetails> doctorQueriedFromDB = doctorRepository.findById(Integer.toString(doctorUserName.hashCode()));
        ArrayList timeSlots = doctorQueriedFromDB.get().getTimeSlots();

        for(Object slot : timeSlots) {
            BasicDBObject ts = new BasicDBObject((LinkedHashMap) slot);
            if (ts.get("date").equals(datestring)) {
                List<Integer> timeSlotIds = (List<Integer>) ts.get("ts");
                timeSlotIds.removeIf(e -> e.equals(timeSlot));
                ((LinkedHashMap) slot).replace("ts", timeSlotIds);
                doctorRepository.save(doctorQueriedFromDB.get());
                break;
            }
        }
    }
}
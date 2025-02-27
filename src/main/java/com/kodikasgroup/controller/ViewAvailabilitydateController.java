package com.kodikasgroup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kodikasgroup.model.Availability;
import com.kodikasgroup.model.Citizen;
import com.kodikasgroup.model.IdReservation;
import com.kodikasgroup.model.Reservation;
import com.kodikasgroup.utils.RequestMaker;
import com.kodikasgroup.utils.UserTempMemory;
import com.kodikasgroup.wrapper.AvailabilityWrapper;
import com.kodikasgroup.wrapper.ReservationWrapper;
import com.kodikasgroup.wrapper.VaccineIdWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.kodikasgroup.App.newWindow;
import static com.kodikasgroup.App.setRoot;

public class ViewAvailabilitydateController {

    private static final String RESERVATIONS_ENDPOINT = "/reservations";
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final int MAX_NUMBER_RESERVATION = 1;
    @FXML
    ListView dateview;
    @FXML
    ListView timetables;

    @FXML
    Label datearea;

    String DEFAULT_CONTROL_INNER_BACKGROUND = "derive(-fx-base,80%)";
    String GREEN_CONTROL_INNER_BACKGROUND = "derive(palegreen, 50%)";
    String RED_CONTROL_INNER_BACKGROUND = "derive(red, 50%)";

    private List<Availability> availability;

    private LocalDate date;
    public int month = 0;
    ReservationWrapper allreservation;
    String clinicName;

    UserTempMemory userTempMemory;

    @FXML
    public void initialize() throws IOException {

        userTempMemory = UserTempMemory.getINSTANCE();

//        //todo: Test Mode isolatio page
//        List<Long> testids = List.of(1L, 2L);
//        VaccineIdWrapper ids = new VaccineIdWrapper(testids);
//        String data = RequestMaker.sendGET("availability" + "/idvaccine" + "?ids=", ids);
//        AvailabilityWrapper result = objectMapper.readValue(data, AvailabilityWrapper.class);
//        availability = new ArrayList<>();
//        for (Availability obj : result.getAvailability()) {
//            availability.add(obj);
//        }
//        userTempMemory.setFiscalcode("BRTCRL30A29E684P");

        //todo: UNCOMMENT and delete test mode
        availability = userTempMemory.getAvailability();
        clinicName = (availability.get(0)).getAvailabilityId().getClinicName();
        date = LocalDate.now();
        getListViewDate();
    }

    private ReservationWrapper getReservationsDay(LocalDate day) throws IOException {

        List<Reservation> allreservation = new ArrayList<>();
        ReservationWrapper temp;
        String recive;
        for (Availability obj : availability) {
            recive = RequestMaker.sendGET(RESERVATIONS_ENDPOINT + "/clinicName/" + URLEncoder.encode(obj.getAvailabilityId().getClinicName(), "UTF-8") +
                    "/idVaccine/" + obj.getAvailabilityId().getIdVaccine().toString() + "/date/" + day);
            temp = objectMapper.readValue(recive, ReservationWrapper.class);
            if (temp != null && temp.getReservations() != null) {
                for (Reservation entry : temp.getReservations()) {
                    if (userTempMemory.getIdVaccines().getIdVaccines().contains(entry.getReservationId().getIdVaccine())) {
                        allreservation.add(entry);
                    }
                }

            }
        }

        return new ReservationWrapper(allreservation);
    }

    private void getListViewDate() {

        LocalDate day;
        if (month != 0) {
            day = date.plusMonths(month);
            day = day.minusDays(date.getDayOfMonth() - 1);
        } else
            day = date;
        datearea.setText(day.getMonth().toString() + "  " + day.getYear());
        LocalDate monthend = day.plusDays((day.lengthOfMonth()) - day.getDayOfMonth());

        //clean List
        dateview.getItems().clear();

        for (; day.isBefore(monthend.plusDays(1)); day = day.plusDays(1)) {
            dateview.getItems().addAll(day);
        }

        dateview.setCellFactory(new Callback<ListView<LocalDate>, ListCell<LocalDate>>() {
            @Override
            public ListCell<LocalDate> call(ListView<LocalDate> param) {
                return new ListCell<LocalDate>() {
                    @Override
                    protected void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                            setStyle("-fx-control-inner-background: " + DEFAULT_CONTROL_INNER_BACKGROUND + ";");
                        } else {
                            setText(item.toString());
                            if (daymanaged(item)) {
                                setStyle("-fx-control-inner-background: " + GREEN_CONTROL_INNER_BACKGROUND + ";");
                            } else {
                                setStyle("-fx-control-inner-background: " + DEFAULT_CONTROL_INNER_BACKGROUND + ";");
                            }
                        }

                    }
                };
            }
        });
    }

    private boolean daymanaged(LocalDate date) {
        for (Availability obj : availability) {
            if (date.isAfter(obj.getStartDate().minusDays(1)) && date.isBefore(obj.getEndDate().plusDays(1)))
                return true;
        }
        return false;
    }

    public void backroclick() {
        month -= 1;
        getListViewDate();
    }

    public void forwardclick() {
        month += 1;
        getListViewDate();
    }

    public void onCLickShowTimetables() throws IOException {

        LocalDate selected = (LocalDate) dateview.getSelectionModel().getSelectedItem();

        if (checkAvailbilityDay(selected)) {
            timetables.getItems().clear();
            printtimetables(selected);
        } else {
            timetables.getItems().clear();
            timetables.getItems().addAll("Non risulta nessuna disponibilitá");
        }
    }

    private boolean checkAvailbilityDay(LocalDate choice) {

        for (Availability obj : availability) {
            if (choice.isBefore(obj.getEndDate().plusDays(1)) && choice.isAfter(obj.getStartDate().minusDays(1)))
                return true;
        }
        return false;

        //return choice.isBefore(availability.getEndDate().plusDays(1)) && choice.isAfter(availability.getStartDate().minusDays(1));
    }

    private void printtimetables(LocalDate day) throws IOException {

        allreservation = getReservationsDay(day);
        for (Availability obj : availability) {
            for (LocalTime time = obj.getStartHour(); time.isBefore(obj.getEndHour().plusHours(1)); time = time.plusHours(1)) {
                timetables.getItems().addAll(time);
            }
        }

        timetables.setCellFactory(new Callback<ListView<LocalTime>, ListCell<LocalTime>>() {
            @Override
            public ListCell<LocalTime> call(ListView<LocalTime> param) {
                return new ListCell<LocalTime>() {
                    @Override
                    protected void updateItem(LocalTime item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                            setStyle("-fx-control-inner-background: " + DEFAULT_CONTROL_INNER_BACKGROUND + ";");
                        } else {
                            setText(item.toString());
                            int check = hourmanaged(item);
                            if (check == 0) {
                                setStyle("-fx-control-inner-background: " + GREEN_CONTROL_INNER_BACKGROUND + ";");
                            } else if (check == 1) {
                                setStyle("-fx-control-inner-background: " + RED_CONTROL_INNER_BACKGROUND + ";");
                            } else {
                                setStyle("-fx-control-inner-background: " + DEFAULT_CONTROL_INNER_BACKGROUND + ";");
                            }
                        }

                    }
                };
            }
        });
    }

    private byte hourmanaged(LocalTime time) {
        // 0 = green , 1 == red
        int numberofreservation = 0;
        if (allreservation != null && allreservation.getReservations() != null) {
            for (Reservation entry : allreservation.getReservations()) {
                if (time.equals(entry.getTime())) {
                    numberofreservation += 1;
                }
            }
        }
        //Number of max prenotations hour
        if (numberofreservation >= MAX_NUMBER_RESERVATION)
            return 1;
        return 0;
    }

    public void onClickConfirme() throws IOException {

        LocalDate localDate = (LocalDate) dateview.getSelectionModel().getSelectedItem();
        LocalTime localtime = (LocalTime) timetables.getSelectionModel().getSelectedItem();

        if (localDate != null || localtime != null) {
            if (hourmanaged(localtime) > 0) {
                //TODO: set Personalized window for this error
                newWindow("unselectedfieldsERROR", 300, 200);
            } else {
                Long idVaccine = getReservationIdVaccine(localDate, localtime);
                Reservation reservation = new Reservation(new IdReservation(idVaccine, userTempMemory.getFiscalcode()), clinicName, localDate, localtime);
                String jsonString = RequestMaker.sendPOST(RESERVATIONS_ENDPOINT, reservation);
                Reservation response = objectMapper.readValue(jsonString, Reservation.class);

                //TODO: set Personalized window for this error
                if (response == null) {
                    newWindow("failedERROR", 300, 200);
                } else {
                    userTempMemory.setClinicname(clinicName);
                    userTempMemory.setLocalDate(localDate);
                    userTempMemory.setLocalTime(localtime);
                    newWindow("done", 900, 650);
                }
            }

        } else {
            //TODO: ERROR MESSAGE nothing selected
        }
    }

    private Long getReservationIdVaccine(LocalDate choice, LocalTime time1) {
        for (Availability obj : availability) {
            if (choice.isBefore(obj.getEndDate().plusDays(1)) && choice.isAfter(obj.getStartDate().minusDays(1))) {
                if (obj.getStartHour().isBefore(time1.plusHours(1)) && obj.getEndHour().isAfter(time1.minusHours(1))) {
                    return obj.getAvailabilityId().getIdVaccine();
                }
            }
        }
        return null;
    }

    public void onCLickBackPage() throws IOException {
        setRoot("availabilitypage");
    }

    public void onClickNotify() throws IOException {
        newWindow("notify", 900, 450);
    }

}
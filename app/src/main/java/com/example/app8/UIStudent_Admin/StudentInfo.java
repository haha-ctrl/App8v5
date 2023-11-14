package com.example.app8.UIStudent_Admin;

public class StudentInfo {
    private String name;
    private String dateOfBirth;
    private String code;
    private byte[] imageData;

    private String attendance_date;
    private int classId;

    public StudentInfo(String name, String code, String dateOfBirth, byte[] imageData) {
        this.name = name;
        this.code = code;
        this.dateOfBirth = dateOfBirth;
        this.imageData = imageData;
    }

    public StudentInfo(String name, String code, String dateOfBirth, String attendance_date, int classId) {
        this.name = name;
        this.code = code;
        this.dateOfBirth = dateOfBirth;
        this.attendance_date = attendance_date;
        this.classId = classId;
    }

    public String getName() {
        return name;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getCode(){
        return code;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public String getAttendance_date() {
        return attendance_date;
    }

    public int getClassId() {
        return classId;
    }
}

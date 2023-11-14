# To install the PROJECT
First, You need create DATABASE
-----------------------------------------------------------------------
    DROP DATABASE PROJECT;
    CREATE DATABASE PROJECT;
    USE PROJECT;

    CREATE TABLE ACCOUNT (
        ID INT IDENTITY(1,1) PRIMARY KEY,
        USERNAME VARCHAR(255) NOT NULL,
        PASSWORD VARCHAR(255) NOT NULL,
        KIND VARCHAR(10) NOT NULL
    );

    CREATE TABLE CLASS (
        id INT PRIMARY KEY IDENTITY(1,1),
        name_class NVARCHAR(255) NOT NULL,
        name_subject NVARCHAR(255) NOT NULL,
        background NVARCHAR(255) NOT NULL
    );
    
    CREATE TABLE THAMGIA (
	classid INT,
	code_student NVARCHAR(255),
	PRIMARY KEY (classid, code_student),
	FOREIGN KEY (classid) REFERENCES CLASS(id),
	FOREIGN KEY (code_student) REFERENCES STUDENT_LIST(code_student)
    );
   
    CREATE TABLE STUDENT_LIST (
	name_student NVARCHAR(255) ,
	code_student NVARCHAR(255) PRIMARY KEY,
	date_of_birth  DATE,
	ImageData VARBINARY(MAX),
	FaceVector VARBINARY(MAX),
	Title NVARCHAR(255),
	Distance FLOAT,
	Id NVARCHAR(255),
    );
    CREATE TABLE ATTENDANCE (
	name_student NVARCHAR(255) ,
	code_student NVARCHAR(255),
	date_of_birth  DATE,
	attendance_date DATE,
	classId INT NOT NULL,
        PRIMARY KEY (classid, code_student),
	FOREIGN KEY (classId) REFERENCES CLASS(id),
	FOREIGN KEY (code_student) REFERENCES STUDENT_LIST(code_student)
    );
 Insert data into the CLASS table.
-----------------------------------------------------------------------
    INSERT INTO CLASS (name_class, name_subject, background) VALUES ('K20-Fetel', 'Lap trinh java', '1');
    INSERT INTO CLASS (name_class, name_subject, background) VALUES ('K20-Fetel', 'He thong nhung', '2');
    INSERT INTO CLASS (name_class, name_subject, background) VALUES ('K20-Fetel', 'Thiet ke Logic kha trinh', '3');
    INSERT INTO CLASS (name_class, name_subject, background) VALUES ('K20-Fetel', 'Ket noi va thu nhan du lieu trong IOT', '4');
    INSERT INTO CLASS (name_class, name_subject, background) VALUES ('K20-Fetel', 'Thiet ke SoC', '5');
    INSERT INTO CLASS (name_class, name_subject, background) VALUES ('K20-Fetel', 'TH lap trinh Java', '6');

 Insert data into the ACCOUNT table.
-----------------------------------------------------------------------
    INSERT INTO ACCOUNT (USERNAME, PASSWORD, KIND) VALUES ('phuc', '123', 'admin');
    INSERT INTO ACCOUNT (USERNAME, PASSWORD, KIND) VALUES ('khanh','456', 'admin');
    INSERT INTO ACCOUNT (USERNAME, PASSWORD, KIND) VALUES ('nghia','789', 'teacher');

Then you need to adjust the IP of the network you are using as your local network, your SQLServer account username and password in the SQLConnection.java and SQLConnectionInBackGround.java files. 
-----------------------------------------------------------------------
      private static String ip = "yourip";
      private static String username = "yourusername";
      private static String password = "yourpassword";


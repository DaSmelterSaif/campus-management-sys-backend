# campus-management-sys-backend

A Spring Boot application for the Campus Management System.

Refer to the [frontend repository](https://github.com/DaSmelterSaif/campus-management-sys-frontend).

## Prerequisites

Before running this project, ensure you have the following installed:

- **Java Development Kit (JDK) version 17 or 21**
- **Git**

## Setup

Run the server (project root):
```shell
.\gradlew.bat bootRun
```

## How to Use

The server runs on http://localhost:8080/

Refer to the [endpoints](#endpoints) section and the
[available credentials](#available-credentials) section...

### Endpoints

- /bookroom (POST)
- /scheduleevents (POST)
- /registerevent (POST)
- /cancelevent (POST)
- /cancelbooking (POST)
- /maintenancerequest (POST)
- /viewmaintenance (POST)
- /approverejectbooking (POST)
- /getstudentfeedback (POST)
- /summarizestudentfeedback (POST)
- /updatemaintenancestatus (POST)
- /login (POST)
- /getbookings (GET)
- /getevents (GET)
- /getmaintenance (GET)
- /getallbookings (GET)
- /getallmaintenance (GET)

### Available Credentials

The following credential list can be used in the frontend
to test the system. Note that registering a new account
was not (and will never be) implemented and the only way
to register is to write the credentials manually.

The credentials are ordered in this format:  
ID,EMAIL,PASSWORD,ROLE,NAME

- 1000,student1@sys.com,pass123,student,John Doe
- 1001,student2@sys.com,pass123,student,Jane Smith
- 200,faculty1@sys.com,pass456,faculty,Dr. Brown
- 201,faculty2@sys.com,pass456,faculty,Prof. Wilson
- 202,maint1@sys.com,pass789,maintenance,Sam Fixer
- 99,admin@sys.com,admin123,admin,System Admin

## Disclaimer

- All the classes written in the com.example.campussysbackend 
package (except for the CampussysbackendApp class) were not 
entirely written by me. The members who helped build it do
not care for credit, and thus, their names has not been 
listed.
- Generative AI may have been used to generate the code
partially (to meet project's tight deadlines).

## License

This project is licensed under the GNU General Public License v3.0 (GPLv3).  
See the [COPYING](COPYING) file for full license details.

For more information about GPLv3, visit [https://www.gnu.org/licenses/gpl-3.0.html](https://www.gnu.org/licenses/gpl-3.0.html)
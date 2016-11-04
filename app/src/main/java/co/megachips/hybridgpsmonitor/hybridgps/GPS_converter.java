package co.megachips.hybridgpsmonitor.hybridgps;


import java.util.Calendar;

/**
 * Created by arita.chihiro on 2016/07/11.
 */
public class GPS_converter {

    private final int GPGGA_DATA_SIZE = 16;
    String TAG = null;

    /**
     * @brief Discriminate GPGGA init"
     * @return Android_GPS_packet format
     */
    public Android_GPS_packet GPGGA_init() {
        Android_GPS_packet gpgga_packet;
        gpgga_packet = new Android_GPS_packet();

        gpgga_packet.time = 0;
        gpgga_packet.satellites_used = 0;
        gpgga_packet.direction_lat = 0;// non
        gpgga_packet.direction_lon = 0;// non
        gpgga_packet.gpgga_hdop = 0;
        gpgga_packet.gpgga_altitude = 0;
        return (gpgga_packet);

    }

    /**
     * @brief Discriminate GPGGA accuracy"
     * @return Android_GPS_packet format
     */
    public Android_GPS_packet GPGGA_converter(String gpgga_data) {
        String[] GPGGA_data = new String[GPGGA_DATA_SIZE];
        convert_GPGGA(gpgga_data, GPGGA_data);
        Android_GPS_packet gpgga_packet;
        gpgga_packet = new Android_GPS_packet();
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int ms = calendar.get(Calendar.MILLISECOND);

        gpgga_packet.time = hour * 60 * 60 * 1000 + minute * 60 * 1000 + second * 1000 + ms;
        gpgga_packet.satellites_used = Long.parseLong(GPGGA_data[7]);
        gpgga_packet.direction_lat = 0;// non
        gpgga_packet.direction_lon = 0;// non
        gpgga_packet.gpgga_hdop = Float.parseFloat(GPGGA_data[8]);
        gpgga_packet.gpgga_altitude = Float.parseFloat(GPGGA_data[9]);
        return (gpgga_packet);

    }


    /**
     * @brief data_length GPGGA format
     * 	GPGGA format
     * 	[0] GPGGA
     * 	[1] CurrentTime
     * 	[2] Latitude
     * 	[3] Latitude Compass Direction
     * 	[4] Longitude
     * 	[5] Longitude Compass Direction
     * 	[6] FixType
     * 	[7] Number Of Satellites
     * 	[8] Horizontal Dilution Of Precision
     * 	[9] Altitude Above
     * 	[10] Altitude Units
     * 	[11] Geoidal Separation
     * 	[12] Units Of Above
     * 	[13] TimeLast Differential Correction
     * 	[14] Differential Station ID
     * 	[15] Checksum
     */
    private void convert_GPGGA(String gpgga_data, String[] GPGGA_data) {
        int data_length;
        int gpgga_index = 0;
        String altitude = "";
        data_length = gpgga_data.length();
        //init String data
        for(int i = 0; i < GPGGA_DATA_SIZE; i++) {
            GPGGA_data[i] = "0";
        }
        //Set GPGGA data
        for(int i = 0; i < data_length; i++) {
            if(gpgga_data.charAt(i) != ',') {
                GPGGA_data[gpgga_index] += gpgga_data.charAt(i);
            }
            else {
                gpgga_index++;
            }
        }
        //Check Altitude Above Sign
        data_length = GPGGA_data[9].length();
        //Set GPGGA data
        for(int i = 0; i < data_length; i++) {
            if(GPGGA_data[9].charAt(i) == '-') {
                altitude = GPGGA_data[9].substring(i, data_length);
                GPGGA_data[9] = altitude;
                break;
            }
        }
    }

    public class Android_GPS_packet{
        public long time;
        public long satellites_used;
        public float direction_lat;
        public float direction_lon;
        public float gpgga_hdop;
        public float gpgga_altitude;
    }
}

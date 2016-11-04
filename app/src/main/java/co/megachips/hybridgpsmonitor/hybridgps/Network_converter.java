package co.megachips.hybridgpsmonitor.hybridgps;

import android.util.Log;
import java.util.*;

public class Network_converter {

    final int DATA_KIND_NUM = 0;
    final int DATA_KIND_START_LON = 1;
    final int DATA_KIND_START_LAT = 2;
    final int DATA_KIND_STOP_LON = 3;
    final int DATA_KIND_STOP_LAT = 4;
    final String end_str = ",0,0";
    String TAG = "Network_converter";

    /**
     * @brief network_converter"
     * @brief pick out start/stop Longitude latitude
     * @param readBinary : NETWORK data
     * @return ArrayList<Highway_data> highway_array : Highway_data data size
     */
    public ArrayList<Highway_data> network_converter(byte[] readBinary) {

        byte [] one_data = new byte [1];
        String str = "";
        String string_data;
        int data_status = DATA_KIND_NUM; //0:num 1:start_lat 2:start_lon 3:stop_lat 4:stop_lon
        Highway_data data = new Highway_data();
        ArrayList<Highway_data> highway_array = new ArrayList<Highway_data>();
        ArrayList<Highway_data> no_data = new ArrayList<Highway_data>();

            for (int i = 0; i < readBinary.length; i++) {
                one_data[0] = readBinary[i];
                string_data = new String(one_data);
                if ((",".equals(string_data)) || ("\n".equals(string_data))) {
                    if ("\n".equals(string_data)) {
                        if(data_status < DATA_KIND_STOP_LON) {
                            return no_data;
                        }
                        try {
                            str = "";
                            data_status = DATA_KIND_NUM;
                            Highway_data add_data = new Highway_data();
                            add_data.start_lat = data.start_lat;
                            add_data.start_lon = data.start_lon;
                            add_data.stop_lat = data.stop_lat;
                            add_data.stop_lon = data.stop_lon;
                            highway_array.add(add_data);
                        }catch(Exception e) {
                            return no_data;
                        }
                    } else {
                        try {
                        if (data_status == DATA_KIND_NUM) {
                            //non
                        } else if (data_status == DATA_KIND_START_LAT) {
                            data.start_lat = Double.parseDouble(str);
                        } else if (data_status == DATA_KIND_START_LON) {
                            data.start_lon = Double.parseDouble(str);
                        } else if (data_status == DATA_KIND_STOP_LAT) {
                            data.stop_lat = Double.parseDouble(str);
                        } else if (data_status == DATA_KIND_STOP_LON) {
                            data.stop_lon = Double.parseDouble(str);
                        } else {
                            //non
                        }
                        data_status++;
                        str = "";
                        }catch(Exception e) {
                            return no_data;
                        }
                    }
                } else {
                    str += string_data;
                }
            }
            return highway_array;
    }

    public class Highway_data {
        public double start_lat;
        public double start_lon;
        public double stop_lat;
        public double stop_lon;
    }
}

package com.javijuol.signalmap.util;

import com.javijuol.signalmap.content.dao.NetworkSignalDAO;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Javier Juan Oltra <javijuol@gmail.com>
 */
public class Preferences {

    public enum FilterDataType {
        FILTER_DATA_2G(NetworkSignalDAO.SIGNAL_TYPE_2G),
        FILTER_DATA_3G(NetworkSignalDAO.SIGNAL_TYPE_3G),
        FILTER_DATA_4G(NetworkSignalDAO.SIGNAL_TYPE_4G),
        FILTER_DATA_ALL(0);


        private final int code;
        private static final Map<Integer,FilterDataType> valuesByCode;

        static {
            valuesByCode = new HashMap<Integer, FilterDataType>();
            for(FilterDataType filterDataType : FilterDataType.values()) {
                valuesByCode.put(filterDataType.code, filterDataType);
            }
        }

        FilterDataType(Integer code) {
            this.code = code;
        }

        public static FilterDataType lookupByCode(Integer code) {
            return valuesByCode.get(code);
        }

        public Integer getCode() {
            return code;
        }
    }


    public static final String PREFERENCE_COLLECTING_DATA = "CollectingData"; // bool
    public static final String PREFERENCE_FILTER_DATA = "FilterData"; // int

}

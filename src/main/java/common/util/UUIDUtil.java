package common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class UUIDUtil {

    public  static String getUUID(){
        return UUID.randomUUID().toString().replaceAll("-","");
    }


    public  static List<String> getUUID(Integer number){
        List<String> list = new ArrayList<>();
        while (0 <= (number--)){
            list.add(getUUID());
        }
        return list;
    }
}

package pl.jsql.enums;

import pl.jsql.api.dto.response.SelectResponse;

import java.util.ArrayList;
import java.util.List;

public enum EncodingEnum {

    MD2,
    MD5,
    SHA1,
    SHA256,
    SHA384,
    SHA512;

    String name;
    String value;

    EncodingEnum(){
        this.name = this.toString();
        this.value = this.toString();
    }

    public static List<SelectResponse<EncodingEnum>> toSelectResponse(){

        List<SelectResponse<EncodingEnum>> list = new ArrayList<>();

        for(EncodingEnum encodingEnum : EncodingEnum.values()){
            list.add(new SelectResponse<>(encodingEnum));
        }

        return list;

    }

}

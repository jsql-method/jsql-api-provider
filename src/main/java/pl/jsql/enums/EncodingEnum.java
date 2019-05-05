package pl.jsql.enums;

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

}

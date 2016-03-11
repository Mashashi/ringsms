package pt.mashashi.ringsms.talk;

public enum MapTypeEnum {
	DEFAULT("1"), PERSONALIZED("0");
	private String code;
	private MapTypeEnum(String code){
		this.code = code;
	}
	public String getCode() {
		return code;
	}
	public static MapTypeEnum getMapInUseByCodeMApId(long idCodeMappings){
		return idCodeMappings!=0?PERSONALIZED:DEFAULT;
	}
}

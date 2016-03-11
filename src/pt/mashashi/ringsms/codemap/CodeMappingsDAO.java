package pt.mashashi.ringsms.codemap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;

import android.content.Context;
import pt.mashashi.ringsms.database.CodeMappingsDataSource;

public class CodeMappingsDAO {
	
	private long id;
	private String mappingsName;
	
	private BiMap<String,String> codesMapping;
	private List<String> usedBy;
	
	public CodeMappingsDAO(long id, String mappingsName, List<String> usedBy){
		this.id=id;
		this.mappingsName=mappingsName;
		this.usedBy = usedBy;
	}
	
	public long getId() {
		return id;
	}
	public String getMappingsName() {
		return mappingsName;
	}
	
	public BiMap<String,String> getCodesMapping(Context ctx, boolean forceRefresh){
		if(codesMapping==null||forceRefresh){
			codesMapping = CodeMappingsDataSource.getInstance(ctx).listCodeMapping(id);
		}
		return codesMapping;
	}
	
	public List<String> getUsedBy(){
		return usedBy;
	}
	
	public String getFile(Context ctx, String... suggestPhoneNumbers){
		StringBuilder buffer = new StringBuilder("");
		Map<String, String> codesMapping = getCodesMapping(ctx, false);
		buffer.append(Arrays.asList(suggestPhoneNumbers).toString());
		buffer.append("\n");
		buffer.append(codesMapping.toString());
		return mappingsName;
	}
	
	
	
}

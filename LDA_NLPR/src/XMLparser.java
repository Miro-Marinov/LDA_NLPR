import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class XMLparser {
    List<File> files = new ArrayList<>();

	public List<ContextList> parse(List<File> files) {		
		List<ContextList> allContextLists = new ArrayList<ContextList>();
		for (File file: files) {
			allContextLists.add(parse(file));
		}
		return allContextLists;
	}
	
	public ContextList parse(File file) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		// get the fileName without the extension
		String fileName = file.getName().substring(file.getName().lastIndexOf("/") + 1, file.getName().lastIndexOf("."));
		File outFile = new File("SemiEval2010 txt/" + fileName + ".txt");
		String targetWord = fileName.substring(0, fileName.indexOf("."));
		ContextList contextList = new ContextList(targetWord);
		
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);
			doc.getDocumentElement().normalize();
			//replace file if exists
			if (outFile.exists()) {
				outFile.delete();
 			}
			outFile.createNewFile();
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
			bw.write(targetWord);
			bw.newLine();
			System.out.println("\nCurrent URI :" + doc.getDocumentURI());
			
			NodeList rootList = doc.getChildNodes();
			
			for (int root = 0; root < rootList.getLength(); root ++) {
				
				Node rNode = rootList.item(root);
				NodeList childList = rNode.getChildNodes();
				
				for (int child = 0; child < childList.getLength(); child ++) {
					Node cNode = childList.item(child);
					contextList.contexts.add(cNode.getTextContent());	
					System.out.println(cNode.getTextContent());
					bw.write(cNode.getTextContent());
					bw.newLine();
					bw.flush();
				}
			}
			bw.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return contextList;
	}
	
	public void getFileNamesInFolder(final File folder) {
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	        	getFileNamesInFolder(fileEntry);
	        } else {
	            files.add(fileEntry);
	        }
	    }
	}
	

	public static void main(String[] args){
		XMLparser parser = new XMLparser();
		parser.getFileNamesInFolder(new File("SemiEval2010 xml"));
		parser.parse(parser.files);
	}
}

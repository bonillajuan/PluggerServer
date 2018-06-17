package pluggerserver;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Brano implements Serializable{

	/**
	 *
	 */
	private static final long serialVersionUID = 2064143847422690207L;

	private transient IntegerProperty idBrano;
	private transient StringProperty titolo;
	private transient StringProperty artista;
	private transient StringProperty album;
	private transient StringProperty pathFile;
	private transient StringProperty pathCover;
	private transient StringProperty durata;
	private transient BooleanProperty hasPathCover;

	public Brano(File file){
		initInstance();
		Path pathFile = file.toPath();
		String stringPath = pathFile.toString();
		setPathFile(stringPath);

		System.out.println("NUOVO BRANO CREATO: "+pathFile);

		//
		UtilityBrano utilityBrano = new UtilityBrano(this, file);
		System.out.println("UTILITY BRANO: "+utilityBrano.toString());

		/*String trackStr = tag.getFirst(FieldKey.TRACK);
		if (trackStr.isEmpty()) { return null; }*/

		/*String artistaTag = tag.getFirst(FieldKey.ARTIST);
		//Artwork artwork = tag.getFirstArtwork();
		String albumTag = tag.getFirst(FieldKey.ALBUM);
		String titoloTag = tag.getFirst(FieldKey.TITLE);
		String pathCover = artwork.getImageUrl();*/

		/*String artista = defaultValue(getTag().getFirst(FieldKey.ARTIST), "Artist");
		String titolo = defaultValue(getTag().getFirst(FieldKey.TITLE), "Title");
		String album = defaultValue(getTag().getFirst(FieldKey.ALBUM), "Album");
		int track = Integer.parseInt(trackStr);*/

        setIdBrano(0);
        setPathCover(null);
        //setPathCover(pathCover);
        //setArtista(artista);
        //setTitolo(titolo);
        //setAlbum(album);
        //setDurata(getLengthMP3(file.toString()));

        System.out.println("TITOLO BRANO: "+getTitolo());
		System.out.println("ARTISTA BRANO: "+getArtista());
		System.out.println("ALBUM BRANO: "+getAlbum());
		System.out.println("PATH FILE: "+getPathFile());
		System.out.println("PATH COVER: "+getPathCover());

	}

	public IntegerProperty idBranoProperty(){ return idBrano; }
	public StringProperty titoloProperty(){ return titolo; }
	public StringProperty artistaProperty(){ return artista; }
	public StringProperty albumProperty(){ return album; }
	public StringProperty pathFileProperty(){ return pathFile; }
	public StringProperty pathCoverProperty(){ return pathCover; }
	public StringProperty durataProperty(){ return durata; }
	public BooleanProperty hasPathCoverProperty(){ return hasPathCover; }

	private void initInstance(){
		idBrano = new SimpleIntegerProperty();
		titolo = new SimpleStringProperty();
		artista = new SimpleStringProperty();
		album = new SimpleStringProperty();
		pathFile = new SimpleStringProperty();
		pathCover = new SimpleStringProperty();
		durata = new SimpleStringProperty();
		hasPathCover = new SimpleBooleanProperty();
	}

	public String getTitolo() {
		return titoloProperty().get();
	}

	public String getArtista() {
		return artistaProperty().get();
	}

	public String getAlbum() {
		return albumProperty().get();
	}

	public String getPathFile() {
		return pathFileProperty().get();
	}

	public String getPathCover() {
		return pathCoverProperty().get();
	}

	public String getDuration(){
		return durataProperty().get();
	}

	public int getIdBrano(){
		return idBranoProperty().get();
	}

	public boolean hasCover(){
		if(getPathCover()!=null){
			System.out.println("PATH COVER: "+getPathCover());
			return true;
		}else{
			System.out.println("PATH COVER: "+getPathCover());
			return false;
		}
	}

	public void setTitolo(String titolo) {
		this.titoloProperty().setValue(titolo);
	}

	public void setArtista(String artista) {
		this.artistaProperty().setValue(artista);
	}

	public void setAlbum(String album) {
		this.albumProperty().setValue(album);
	}

	public void setPathFile(String pathFile) {
		this.pathFileProperty().setValue(pathFile);
	}

	public void setPathCover(String pathCover) {
		this.pathCoverProperty().setValue(pathCover);
		if(getPathCover()!=null){
			setHasCover(true);
		}else{
			setHasCover(false);
		}
	}

	public void setDurata(String duration) {
		this.durataProperty().setValue(duration);
	}

	public void setIdBrano(int idBrano){
		this.idBranoProperty().setValue(idBrano);
	}

	public void setHasCover(boolean check){
		this.hasPathCoverProperty().setValue(check);
	}

	@Override
	public String toString(){
		return "BRANO: "+getTitolo()+", ARTISTA: "+getArtista()+", ALBUM: "+getAlbum();
	}

	private void writeObject(ObjectOutputStream out) throws IOException{
		out.defaultWriteObject();
		out.writeInt(idBranoProperty().intValue());

		out.writeUTF(titoloProperty().getValueSafe());
		out.writeUTF(artistaProperty().getValueSafe());
		out.writeUTF(albumProperty().getValueSafe());
		out.writeUTF(pathFileProperty().getValueSafe());
		out.writeUTF(pathCoverProperty().getValueSafe());
		out.writeUTF(durataProperty().getValueSafe());
		out.writeBoolean(hasPathCoverProperty().getValue());
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
		in.defaultReadObject();
		initInstance();

		setIdBrano(in.readInt());
		setTitolo(in.readUTF());
		setArtista(in.readUTF());
		setAlbum(in.readUTF());
		setPathFile(in.readUTF());
		setPathCover(in.readUTF());
		setDurata(in.readUTF());
		setHasCover(in.readBoolean());
	}
}

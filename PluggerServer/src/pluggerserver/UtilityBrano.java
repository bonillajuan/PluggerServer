package pluggerserver;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;

public class UtilityBrano {

	private MP3File audioFile;
	private AbstractID3v2Tag tag;
	private Brano brano;

	public UtilityBrano(Brano brano, File file){

		setBrano(brano);

		try {
			System.out.println("BRANO: "+getBrano().toString()+", FILE: "+file);
			MP3File audioFile = (MP3File)AudioFileIO.read(file);
			setAudioFile(audioFile);
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException
				| InvalidAudioFrameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (getAudioFile() != null && getAudioFile().hasID3v2Tag()) {
			AbstractID3v2Tag tag = getAudioFile().getID3v2Tag();
            setTag(tag);
        }

		String artista = defaultValue(getTag().getFirst(FieldKey.ARTIST), "Artist");
		String titolo = defaultValue(getTag().getFirst(FieldKey.TITLE), "Title");
		String album = defaultValue(getTag().getFirst(FieldKey.ALBUM), "Album");

		//int track = Integer.parseInt(trackStr);

		getBrano().setTitolo(titolo);
		getBrano().setArtista(artista);
		getBrano().setAlbum(album);
		getBrano().setDurata(getLengthMP3());

	}

	private Brano getBrano() {
		return this.brano;
	}

	private void setBrano(Brano brano) {
		this.brano = brano;
	}

	public void setTagTitolo(String titolo) {
		 try {
			System.out.println("TITOLO: "+titolo);
            getTag().setField(FieldKey.TITLE, titolo);
            getAudioFile().setTag(tag);
            getAudioFile().commit();
         	System.out.println("NUOVO META TITOLO: "+tag.getFirst(FieldKey.TITLE));
        } catch (KeyNotFoundException e) {
            e.printStackTrace();
        } catch (FieldDataInvalidException e) {
            e.printStackTrace();
        } catch (CannotWriteException e) {
            e.printStackTrace();
        }
	}

	public String getTagTitolo(){
		String titolo = getTag().getFirst(FieldKey.TITLE);
		return titolo;
	}

	public String getTagArtista(){
		String artista = getTag().getFirst(FieldKey.ARTIST);
		return artista;
	}

	public String getTagAlbum(){
		String album = getTag().getFirst(FieldKey.ALBUM);
		return album;
	}

	public void setTagArtista(String artista) {
		try {
           getTag().setField(FieldKey.ARTIST, artista);
           getAudioFile().setTag(tag);
           getAudioFile().commit();
           System.out.println("NUOVO META ARTISTA: "+tag.getFirst(FieldKey.ARTIST));
       } catch (KeyNotFoundException e) {
           e.printStackTrace();
       } catch (FieldDataInvalidException e) {
           e.printStackTrace();
       } catch (CannotWriteException e) {
           e.printStackTrace();
       }
	}

	public void setTagAlbum(String album) {
		try {
           tag.setField(FieldKey.ALBUM, album);
           audioFile.setTag(tag);
           audioFile.commit();
           System.out.println("NUOVO META ALBUM: "+tag.getFirst(FieldKey.ALBUM));
       } catch (KeyNotFoundException e) {
           e.printStackTrace();
       } catch (FieldDataInvalidException e) {
           e.printStackTrace();
       } catch (CannotWriteException e) {
           e.printStackTrace();
       }
	}

	public String getLengthMP3(){
		java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);

		String duration = getAudioFile().getMP3AudioHeader().getTrackLengthAsString();
		System.out.println("DURATA IN MINUTI MP3: "+duration);
		return duration;
	}

	public void setAudioFile(MP3File audioFile){
		this.audioFile = audioFile;
	}

	public MP3File getAudioFile(){
		return this.audioFile;
	}

	public void setTag(AbstractID3v2Tag tag){
		this.tag = tag;
	}

	public AbstractID3v2Tag getTag(){
		return this.tag;
	}

	private String defaultValue(String value, String name) {
        if (value == null || value.isEmpty()) {
            return String.format("Unknown %s", name);
        }
        return value;
	}

	@Override
	public String toString(){
		return "Brano Titolo: "+getTagTitolo()+", Artista: "+getTagArtista()+", Album: "+getTagAlbum();
	}
}

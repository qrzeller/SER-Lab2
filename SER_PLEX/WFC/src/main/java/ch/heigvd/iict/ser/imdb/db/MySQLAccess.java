package ch.heigvd.iict.ser.imdb.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ch.heigvd.iict.ser.imdb.models.Data;
import ch.heigvd.iict.ser.imdb.models.Movie;
import ch.heigvd.iict.ser.imdb.models.Person;
import ch.heigvd.iict.ser.imdb.models.Role;
import ch.heigvd.iict.ser.imdb.models.Person.Gender;

public class MySQLAccess {

	public static String MYSQL_URL 						= null;
	public static String MYSQL_DBNAME 					= null;
	public static String MYSQL_USER 					= null;
	public static String MYSQL_PASSWORD 				= null;

	private Connection connection 						= null;

	private PreparedStatement movieStatement 			= null;
	private PreparedStatement movieInfosStatement 		= null;
	private PreparedStatement movieKeywordsStatement 	= null;
	private PreparedStatement movieCastStatement 		= null;
	private PreparedStatement movieCharacterCastStatement = null;

	private PreparedStatement personNameStatement 		= null;
	private PreparedStatement personAkaNameStatement 	= null;
	private PreparedStatement personInfosStatement 		= null;

	public MySQLAccess() {
		if(isStringNullOrEmpty(MYSQL_URL) || isStringNullOrEmpty(MYSQL_DBNAME) || isStringNullOrEmpty(MYSQL_USER) || isStringNullOrEmpty(MYSQL_PASSWORD)) {
			System.err.println("MySQL configuration is missing or incomplete");
			System.exit(1);
		}
	}

	/**
	 * 	Method used to open connection with database
	 */
	public void connect() {
		try {
			connection = DriverManager.getConnection("jdbc:mysql://" + MYSQL_URL + "/" + MYSQL_DBNAME + "?" + "user=" + MYSQL_USER + "&password=" + MYSQL_PASSWORD + "");
		} catch (SQLException e) {
			System.err.println("Cannot connect to DB: " + e.getMessage());
			System.exit(1);
		}

		prepareQueries();
	}

	public Data getData(int version) {
		Data data = new Data();

		Set<Long> personsIds = new TreeSet<Long>();
		List<Integer> moviesId = null;

		switch(version) {
		case 1:
			moviesId = Constants.MOVIES_IDS_1ST;
			data.setVersion(1L);
			break;
		case 2:
			moviesId = Constants.MOVIES_IDS_2ND;
			data.setVersion(2L);
			break;
		case 3:
			moviesId = Constants.MOVIES_IDS_3RD;
			data.setVersion(3L);
			break;
		}

		if(moviesId == null) {
			System.err.println("getData() - Requested version does not exits");
			return null;
		}

		//we prepare data
		data.setMovies(new HashMap<Long, Movie>());
		data.setPersons(new HashMap<Long, Person>());
		try {

			for(Integer movieId : moviesId) {
				this.movieStatement.setInt(1, movieId);

				ResultSet rsMovie = this.movieStatement.executeQuery();

				if(rsMovie.first()) {
					Movie movie = new Movie();

					//only one movie in results
					Long movieDbId = rsMovie.getLong(1);
					movie.setId(movieDbId);
					movie.setTitle(rsMovie.getString(2));
					movie.setImage(Constants.getImageUrlForMovie(movieId));
					
					//other informations
					this.movieInfosStatement.setLong(1, movieDbId);
					ResultSet rsMovieInfos = this.movieInfosStatement.executeQuery();

					//init
					movie.setGenres(new HashSet<String>());
					movie.setLanguages(new HashSet<String>());

					while(rsMovieInfos.next()) {
						int infoType = rsMovieInfos.getInt(3);
						String info = rsMovieInfos.getString(4);
						String note = rsMovieInfos.getString(5);

						if(info == null || info.isEmpty()) continue;

						switch(infoType) {
						case Constants.INFO_RUNTIMES:
							try{
								movie.setRuntime(Integer.parseInt(info));
							} catch(Exception e) {}
							break;
						case Constants.INFO_GENRES:
							movie.getGenres().add(info);
							break;
						case Constants.INFO_LANGUAGES:
							movie.getLanguages().add(info);
							break;
						case Constants.INFO_PLOT:
							if(movie.getPlot() == null || movie.getPlot().isEmpty() || info.length() > movie.getPlot().length()) {
								movie.setPlot(info);
							}
							break;
						case Constants.INFO_RATING:
							System.out.println("\t"+"INFO_RATING: " + info + " [" + note + "]");
							break;
						case Constants.INFO_BUDGET:
							movie.setBudget(info);
							break;
							//case Constants.INFO_ADMISSIONS: break; NOT USEABLE
						}

					}
					rsMovieInfos.close();

					//keywords
					this.movieKeywordsStatement.setLong(1, movieDbId);
					ResultSet rsMovieKeywords = this.movieKeywordsStatement.executeQuery();

					//init
					movie.setKeywords(new HashSet<String>());

					while(rsMovieKeywords.next()) {
						String keyword = rsMovieKeywords.getString(1);
						if(keyword != null) {
							movie.getKeywords().add(keyword);
						}
					}
					rsMovieKeywords.close();

					//list persons ids
					this.movieCastStatement.setLong(1, movieDbId);
					ResultSet rsMovieCast = this.movieCastStatement.executeQuery();

					//init
					movie.setActorsIds(new TreeSet<Role>());
					movie.setDirectorsIds(new HashSet<Long>());
					movie.setEditorsIds(new HashSet<Long>());
					movie.setProducersIds(new HashSet<Long>());
					movie.setWritersIds(new HashSet<Long>());

					while(rsMovieCast.next()) {

						long personId = rsMovieCast.getLong(2);
						int charId = rsMovieCast.getInt(4);
						long order = rsMovieCast.getLong(6);
						int roleId = rsMovieCast.getInt(7);

						boolean keepReferenceToPerson = false;
						
						switch(roleId) {
						case Constants.ROLE_ACTOR:
						case Constants.ROLE_ACTRESS:
							keepReferenceToPerson = true;
							Role role = new Role();
							role.setActorId(personId);
							role.setOrder(order);

							//we recover the corresponding character name
							this.movieCharacterCastStatement.setInt(1, charId);
							ResultSet rsCharacter = this.movieCharacterCastStatement.executeQuery();
							if(rsCharacter.first()) { //only one result
								String characterName = rsCharacter.getString(1);
								role.setCharacter(characterName);
							}
							rsCharacter.close();
							movie.getActorsIds().add(role);
							break;
						case Constants.ROLE_PRODUCER:
							keepReferenceToPerson = true;
							movie.getProducersIds().add(personId);
							break;
						case Constants.ROLE_WRITER:
							keepReferenceToPerson = true;
							movie.getWritersIds().add(personId);
							break;
						case Constants.ROLE_DIRECTOR:
							keepReferenceToPerson = true;
							movie.getDirectorsIds().add(personId);
							break;
						case Constants.ROLE_EDITOR:
							keepReferenceToPerson = true;
							movie.getEditorsIds().add(personId);
							break;
						}

						//we add the person id to the persons list
						if(keepReferenceToPerson){
							personsIds.add(personId);
						}

					}
					rsMovieCast.close();
					data.getMovies().put(movie.getId(), movie);
				}

				rsMovie.close();
			}

			//we load person infos
			for(Long personId: personsIds) {
				this.personNameStatement.setLong(1, personId);
				ResultSet rsPersonName = this.personNameStatement.executeQuery();

				if(rsPersonName.first()) { //only one result

					Person person = new Person();
					person.setId(personId);
					person.setName(rsPersonName.getString(1));
					person.setImage(Constants.getImageUrlForPerson(personId));
					
					String gender = rsPersonName.getString(2);
					if("m".equalsIgnoreCase(gender)) person.setGender(Gender.MALE);
					else if("f".equalsIgnoreCase(gender)) person.setGender(Gender.FEMALE);
					else person.setGender(Gender.UNKNOWN);

					//aka names
					person.setAkaNames(new LinkedList<String>());

					this.personAkaNameStatement.setLong(1, personId);
					ResultSet rsAkaName = this.personAkaNameStatement.executeQuery();

					while(rsAkaName.next()) {
						String akaName = rsAkaName.getString(1);
						person.getAkaNames().add(akaName);
					}
					rsAkaName.close();

					//person infos
					this.personInfosStatement.setLong(1, personId);
					ResultSet rsPersonInfos = this.personInfosStatement.executeQuery();

					while(rsPersonInfos.next()) {
						int type = rsPersonInfos.getInt(3);
						String info = rsPersonInfos.getString(4);
						//String note = rsPersonInfos.getString(5);

						switch(type) {
						case Constants.INFO_MINI_BIOGRAPHY:
							person.setBiography(info);
							break;
						case Constants.INFO_BIRTHDATE:
							person.setBirthDateAsString(info);
							break;
						case Constants.INFO_DEATHDATE:
							person.setDeathDateAsString(info);
							break;
						case Constants.INFO_BIRTHNAME:
							person.setBirthName(info);
							break;
						}

					}
					rsPersonInfos.close();
					data.getPersons().put(personId, person);
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Error while requesting in database: " + e.getMessage());
		}

		return data;
	}

	/**
	 * 	Methode used to close connection with database
	 */
	public void disconnect() {
		closePreparedQueries();
		try {
			this.connection.close();
		} catch (SQLException e) {
			System.err.println("Error while disconnecting from DB: " + e.getMessage());
		}
	}

	private void prepareQueries() {
		try {
			this.movieStatement = this.connection.prepareStatement("SELECT * FROM `title` WHERE `id` = ?");
			this.movieInfosStatement = this.connection.prepareStatement("SELECT * FROM `movie_info` WHERE `movie_id` = ?");
			this.movieKeywordsStatement = this.connection.prepareStatement("SELECT `keyword` FROM `movie_keyword`, `keyword` WHERE `movie_keyword`.`movie_id` = ? AND `movie_keyword`.`keyword_id` = `keyword`.`id`");
			this.movieCastStatement = this.connection.prepareStatement("SELECT * FROM `cast_info` WHERE `movie_id` = ?");
			this.movieCharacterCastStatement = this.connection.prepareStatement("SELECT `name` FROM `char_name` WHERE `id` = ?");
			this.personNameStatement = this.connection.prepareStatement("SELECT `name`, `gender` FROM `name` WHERE `id` = ?");
			this.personAkaNameStatement = this.connection.prepareStatement("SELECT `name` FROM `aka_name` WHERE `person_id` = ?");
			this.personInfosStatement = this.connection.prepareStatement("SELECT * FROM `person_info` WHERE `person_id` = ?");
		} catch (SQLException e) {
			System.err.println("Error while preparing queries");
			System.exit(1);
		}
	}
	
	private void closePreparedQueries() {
		try {
			this.movieCharacterCastStatement.close();
			this.movieCastStatement.close();
			this.movieKeywordsStatement.close();
			this.movieInfosStatement.close();
			this.movieStatement.close();
			this.personInfosStatement.close();
			this.personAkaNameStatement.close();
			this.personNameStatement.close();
		} catch (SQLException e) {
			System.err.println("Error while closinf prepared queries: " + e.getMessage());
		}
	}
	
	private boolean isStringNullOrEmpty(String s) {
		return s == null || s.isEmpty();
	}

}

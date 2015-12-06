import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnector {
	private Connection con;
	private Statement st;
	private ResultSet rs;
	private int rs2;
	private String idZwiazku;
	private int direction;

	public DBConnector() {
		try {
			// Class.forName("com.mysql.jdbc.Driver");

			con = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/TIBI", "root", "abcd1234");

			st = con.createStatement();

		} catch (Exception ex) {
			System.out.println("Error: " + ex);
		}
	}

	public void getData() {
		try {
			String querry = "Select * from TIBI.markers";
			rs = st.executeQuery(querry);
			System.out.println("Records from database: ");
			while (rs.next()) {
				String name = rs.getString("name");
				String descr = rs.getString("descr");

				System.out.println("Name: " + name + " " + "descr: " + descr);
			}

		} catch (Exception ex) {
			System.out.println(ex);
		}
	}

	public int allowOpen(String givenCode, char mode) {
		try {
			System.out.println("Message from LPC:" + givenCode + " mode: " + mode);

			direction = 0;

			if (mode == ')') { // Wjazd
				// Czy ma upranienia do wjazdu
				String querry = "SELECT status, idosoba_samochod FROM Rejestracje.Osoba left join Rejestracje.`Osoba-Samochod` on (idOsoba=id_Osoba) left join Rejestracje.Kod_IR on(id_Kod_IR=idKod) where kod = "
						+ givenCode + ";";

				rs = st.executeQuery(querry);

				if (!rs.next()) {
					System.out.println("pusto");
					return 2;
				}

				String status;
				rs.previous();
				while (rs.next()) {
					status = rs.getString("status");
					if (status.equals("1") || status.equals("2")) {
						System.out.println(this.getidZwiazku());

						setidZwiazku(rs.getString("idosoba_samochod"));
						System.out.println(this.getidZwiazku());
						// return 0;

					} else if (status.equals("3")) {
						return 4;
					}
				}

				// Sprawdzenie czy kod nie jest w użyciu
				querry = "SELECT `Wjechanie`.`idwjechania`" + "FROM `Rejestracje`.`Wjechanie`"
						+ "inner join `Rejestracje`.`Osoba-Samochod`" + "on(idosoba_samochod = id_Zwiazku)"
						+ "inner join `Rejestracje`.`Kod_IR`" + "on(idKod=id_Kod_IR)" + "where kod = " + givenCode
						+ " and Na_parkingu = 1;";

				rs = st.executeQuery(querry);

				while (rs.next()) {

					return 3; // Kod w użyciu
				}

				direction = 1;
				return 0;

			} else if (mode == '+') { // Wyjazd
				// Czy na ten kod ktoś wjechał
				String querry = "SELECT `Wjechanie`.`id_Zwiazku`, `zakaz_wyjazdu` " + "FROM `Rejestracje`.`Wjechanie`"
						+ "inner join `Rejestracje`.`Osoba-Samochod`" + "on(idosoba_samochod = id_Zwiazku)"
						+ "inner join `Rejestracje`.`Kod_IR`" + "on(idKod=id_Kod_IR)" + "where kod = " + givenCode
						+ " and Na_parkingu = 1;";

				rs = st.executeQuery(querry);

				while (rs.next()) {

					if (rs.getInt("zakaz_wyjazdu") == 1)
						return 6;
					else {
						setidZwiazku(rs.getString("id_Zwiazku"));
						direction = 2;
						return 0; // Zezwolenie na wyjazd
					}
				}

				querry = "SELECT zakaz_wyjazdu, `idosoba_samochod`  FROM `Rejestracje`.`Osoba-Samochod`"
						+ "inner join `Rejestracje`.`Kod_IR` " + "on(idKod=id_Kod_IR) " + "where kod = " + givenCode
						+ ";";
				rs = st.executeQuery(querry);

				while (rs.next()) {
					if (rs.getInt("zakaz_wyjazdu") == 1)
						return 6;
					else {
						setidZwiazku(rs.getString("idosoba_samochod"));
						direction = 2;
						return 0; // Zezwolenie na wyjazd
					}
				}

				return 2;
			}

		} catch (Exception ex) {
			System.out.println(ex);
			// ex.printStackTrace();
		}
		direction = 0;
		return 1;
	}

	public void sendLog() {
		try {
			if (direction == 1) { // wjazd
				String query = "INSERT INTO `Rejestracje`.`Wjechanie`(`czasWjazdu`,"
						+ "`id_Zwiazku`,`Na_parkingu`)VALUES(now()," + idZwiazku + ",1);";
				rs2 = st.executeUpdate(query);

				idZwiazku = "0";
				System.out.println("Log sent - zanotowanie wjazdu");
			} else if (direction == 2) { // wyjazd
				String query = "SELECT * FROM Rejestracje.Wjechanie " + "WHERE id_Zwiazku = " + idZwiazku
						+ " AND Na_parkingu = 1;";

				rs = st.executeQuery(query);

				while (rs.next()) {
					query = "" + " UPDATE `Rejestracje`.`Wjechanie` SET czasWyjazdu = now(), Na_parkingu = 2 "
							+ " where `id_Zwiazku` = " + idZwiazku + " and Na_parkingu = 1;";

					rs2 = st.executeUpdate(query);
					System.out.println("Log sent - zanotowanie wYjazdu ");
					return;
				}

				query = "INSERT INTO `Rejestracje`.`Wjechanie`" + "(`czasWyjazdu`,`id_Zwiazku`,`Na_parkingu`)"
						+ "VALUES(NOW(),"+idZwiazku+",2);";

				rs2 = st.executeUpdate(query);

				idZwiazku = "0";
				System.out.println("Log sent - zanotowanie wYjazdu samochodu który nie miał odnotowaniego wjazdu");
			} else { // nie wiadomo
				String query = "INSERT INTO `Rejestracje`.`Wjechanie`(`czasWjazdu`,`czasWyjazdu`,"
						+ "`id_Zwiazku`,`Na_parkingu`)VALUES(now(),now(),0,0);";
				rs2 = st.executeUpdate(query);

				idZwiazku = "0";
				System.out.println("Log sent - don't know who and which way");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public String getidZwiazku() {
		return idZwiazku;
	}

	public void setidZwiazku(String idZwiazku) {
		this.idZwiazku = idZwiazku;
	}

}
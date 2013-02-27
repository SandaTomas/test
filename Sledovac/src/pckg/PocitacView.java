package pckg;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.*;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * 
 * @author tsanda
 */
public class PocitacView extends javax.swing.JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5408596137914243525L;

	/** Creates new form PocitacView */
	public PocitacView() {

		setLookAndFeel();

		LoadPosition();

		initComponents();

		trayicon();

		this.setVisible(true);
		// this.AbovePanel.setVisible(false);
		this.FakturovatCheckBox.setSelected(true);
		
		System.out.println("Nasdtavuji sirka = "+sirka + " vyska = "+vyska);

//		this.setMinimumSize(new Dimension(sirka, vyska));
//		this.setPreferredSize(new Dimension(sirka, vyska));
//		this.setMaximumSize(new Dimension(sirka, vyska));
		this.setMinimumSize(new Dimension(1000, 500));
		this.setPreferredSize(new Dimension(1000, 500));
		this.setMaximumSize(new Dimension(1000, 500));

		Image im = Toolkit.getDefaultToolkit().getImage("Data\\time.png");
		im = im.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
		this.setIconImage(im);

		this.statusMessageLabel.setForeground(Color.LIGHT_GRAY);

		// Pridej sloupce do tabulky
		model.addColumn("ID");
		model.addColumn("Firma");
		model.addColumn("Popis");
		model.addColumn("Èas spuštìní");
		model.addColumn("Stráveno");
		model.addColumn("Spuštìno");
		model.addColumn("Fakturovat");

		this.Table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		setColumnWidth(kratky, stredni, dlouhy);

		RefreshTable(model, AKTIVNI);

		// nastaveni casoveho pasma, nevim, jestli je to takto spravne, ale
		// funguje to :)
		sdf.setTimeZone(TimeZone.getTimeZone("CET"));
		df.setTimeZone(TimeZone.getTimeZone("CEST"));

	}

	/****************************************************
	 * DEFINICE PROMENNYCH
	 ****************************************************/

	/** Vychozi nastaveni vysky a sirky okna */
	int sirka = 1000;
	int vyska = 500;

	private int pocetSloupcu = 7;
	/**
	 * Hodnota udava cislo platne pro stav ulohy, ktera neni v archivu, je
	 * normalne aktivni.
	 */
	private int AKTIVNI = 0;
	/**
	 * Hodnota udava cislo platne pro stav ulohy, ktera je jiz presunuta do
	 * archivu..
	 */
	private int ARCHIV = 1;
	/** Udava stav, ktery je aktivovan na hlavni tabulce. AKTIVNI/ARCHIV */
	private int STAV;
	/** Hodnota udava cislo platne pro ulohu, ktera neni spustena. */
	private int ZASTAVENO = 0;
	/** Hodnota udava cislo platne pro ulohu, ktera je spustena. */
	private int SPUSTENO = 1;
	/** Nejmensi sirka sloupce v tabulce ukolu */
	private int kratky = 30;
	/** Prostredni sirka sloupce v tabulce ukolu */
	private int stredni = 80;
	/** Nejvetsi sirka sloupce v tabulce ukolu */
	private int dlouhy = 150;
	/** Definice modelu tabulky ukolu */
	DefaultTableModel model = new DefaultTableModel() {
		// definice, ktery sloupce budou editovatelne pokud je podminka <
		// pocetSloupcu
		// neni editovatelny zadny z nich

		private static final long serialVersionUID = 6642323686385479029L;

		@Override
		public boolean isCellEditable(int rowIndex, int mColIndex) {
			if (mColIndex < pocetSloupcu) {
				return false;
			} else {
				return true;
			}
		}

		Class[] types = new Class[] { java.lang.String.class,
				java.lang.String.class, java.lang.String.class,
				java.lang.String.class, java.lang.String.class,
				java.lang.Boolean.class, java.lang.Boolean.class };

		@Override
		public Class getColumnClass(int columnIndex) {
			return types[columnIndex];
		}
	};
	private static SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	private static SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

	/**
	 * Smaze vsechny radky z tabulky v danem modelu
	 * 
	 * @param DefModel
	 *            - model tabulky, ktera se ma vymazat
	 */
	private void DellTable(DefaultTableModel DefModel) {
		// deletes ALL the rows
		DefModel.getDataVector().removeAllElements();
		// repaints the table and notify all listeners (only once!)
		DefModel.fireTableDataChanged();
	}

	/**
	 * Nacte znovu vsechny sloupce y databaze ukoly do tabulky
	 * 
	 * @param model
	 *            - model tabulky, ktera se ma obnovit
	 * @param type
	 *            - typ dat, ktera se maji nacist do tabulky 0 - normalni, 1 -
	 *            archiv dokoncenych
	 */
	private void RefreshTable(DefaultTableModel model, int type) {
		try {
			DellTable(model);

			PreparedStatement pst = PocitacApp.con
					.prepareStatement("SELECT * FROM Ukoly WHERE Dokonceno=(?) ORDER BY ID");

			pst.setInt(1, type);

			PocitacApp.rs = pst.executeQuery();

			boolean spusteno = false;
			boolean fakturovat = false;

			while (PocitacApp.rs.next()) {

				// podle hodnoty 0, 1 se rozhodne jesli bude checkbox zaskrtly
				// nebo ne
				if (PocitacApp.rs.getString("Spusteno").equals("1")) {
					spusteno = true;
				} else {
					spusteno = false;
				}
				if (PocitacApp.rs.getString("Fakturovat").equals("1")) {
					fakturovat = true;
				} else {
					fakturovat = false;
				}

				model.addRow(new Object[] { PocitacApp.rs.getString("ID"),
						PocitacApp.rs.getString("Firma"),
						PocitacApp.rs.getString("Popis"),
						PocitacApp.rs.getString("KdySpusteno"),
						PocitacApp.rs.getString("Cas"), spusteno, fakturovat });
			}

			// po vlozeni je potreba vymazat pole poznamky
			this.PopisTextField.setText("");
			this.FakturovatCheckBox.setSelected(true);
			this.FirmaComboBox.requestFocusInWindow();
			
			if (!FilterFirmaTextField.equals("") || !FilterPopisTextField.equals("")) {
				java.awt.event.ActionEvent evt = null;
				SearchButtonActionPerformed(evt);
			}

		} catch (SQLException ex) {
			this.statusMessageLabel.setText("Chyba pøi komunikaci SQL: "
					+ ex.getMessage());
		} catch (Exception ex) {
			this.statusMessageLabel.setText("Všeobecná chyba: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	private int StopActualJob() {
		try {

			PreparedStatement pst = PocitacApp.con
					.prepareStatement("SELECT * FROM Ukoly WHERE Spusteno=(?) ORDER BY ID");

			pst.setInt(1, SPUSTENO);

			PocitacApp.rs = pst.executeQuery();

			Calendar cal = Calendar.getInstance();
			Date datum;
			Date time;
			String tempCas = "";
			/**
			 * DORESIT PREPOCET CASU - ODECITANI OD - DO pri nenajiti ulohy
			 * ktera by byla spustena vrat 0
			 */
			while (PocitacApp.rs.next()) {

				System.out.println("Mame zaznamy");

				time = new Date(0);
				tempCas = PocitacApp.rs.getString("Cas");
				datum = sdf.parse(PocitacApp.rs.getString("KdySpusteno")
						.toString());

				if (String.valueOf(tempCas).equals("null")) {
					// novy cas
				} else {
					// pricitej
					time = df.parse(tempCas.toString());
				}

				long straveno = cal.getTimeInMillis() - datum.getTime()
						+ time.getTime();

				pst = PocitacApp.con
						.prepareStatement("UPDATE Ukoly SET Cas = (?), Spusteno = (?) WHERE ID = (?)");

				pst.setString(1, df.format(straveno).toString());
				pst.setInt(2, ZASTAVENO);
				pst.setString(3, PocitacApp.rs.getString("ID"));

				this.statusMessageLabel.setText("Na úloze bylo odpracováno : "
						+ df.format(straveno).toString());

				pst.executeUpdate();
			}

			RefreshTable(model, STAV);

			return 1; // uspech - job zastaven

		} catch (SQLException ex) {
			this.statusMessageLabel.setText("Chyba pøi komunikaci SQL: "
					+ ex.getMessage());
		} catch (ParseException ex) {
			this.statusMessageLabel.setText("Chyba parsování èasu: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (Exception ex) {
			this.statusMessageLabel.setText("Všeobecná chyba: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		}

		return 0; // nepovedlo se
	}

	/**
	 * Vrati aktulni cas
	 * 
	 * @return
	 */
	private String now() throws ParseException {
		Calendar cal = Calendar.getInstance();
		return sdf.format(cal.getTime());
	}

	/**
	 * Zjistim, zda je spustena nehaka uloha
	 * 
	 * @return false pokud neni spustena, true pokud je spustena
	 */
	boolean Spusteno() {

		try {
			PreparedStatement pst = PocitacApp.con
					.prepareStatement("SELECT * FROM Ukoly WHERE Spusteno=(?)");

			pst.setInt(1, SPUSTENO);

			PocitacApp.rs = pst.executeQuery();

			while (PocitacApp.rs.next()) {
				return true;
			}

		} catch (SQLException ex) {
			this.statusMessageLabel.setText("Chyba pøi komunikaci SQL: "
					+ ex.getMessage());
		} catch (Exception ex) {
			this.statusMessageLabel.setText("Všeobecná chyba: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		}
		return false;
	}

	private void setTlacitkovyPanel(boolean stav) {
		this.InsertButton.setEnabled(stav);
		this.InsertStartButton.setEnabled(stav);
		this.StartButton.setEnabled(stav);
		this.StopButton.setEnabled(stav);
		this.FinishButton.setEnabled(stav);
		// this.DellButton.setEnabled(stav);
	}

	private void Konec() {
		try {
			SavePosition();
			if (!PocitacApp.con.isClosed()) {
				PocitacApp.con.close();
			}
			System.exit(0);
		} catch (SQLException ex) {
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
			System.exit(1);
		}
	}

	private void SavePosition() {
		PreparedStatement pst;
		try {
			pst = PocitacApp.con
					.prepareStatement("UPDATE Setup SET Hodnota = (?) WHERE Typ = (?)");

			pst.setInt(1, getSize().width);
			pst.setString(2, "sirka");
			pst.executeUpdate();
			
			pst.setInt(1, getSize().height);
			pst.setString(2, "vyska");
			pst.executeUpdate();

		} catch (SQLException ex) {
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	private void LoadPosition() {
		PreparedStatement pst;
		try {
			pst = PocitacApp.con
					.prepareStatement("SELECT Hodnota FROM Setup WHERE Typ=(?)");
			pst.setString(1, "sirka");
			PocitacApp.rs = pst.executeQuery();
			while (PocitacApp.rs.next()) {
				sirka = Integer.parseInt(PocitacApp.rs.getString("Hodnota"));
			}

			pst.setString(1, "vyska");
			PocitacApp.rs = pst.executeQuery();
			while (PocitacApp.rs.next()) {
				vyska = Integer.parseInt(PocitacApp.rs.getString("Hodnota"));
			}

		} catch (SQLException ex) {
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	private void setLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (InstantiationException ex) {
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (IllegalAccessException ex) {
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (UnsupportedLookAndFeelException ex) {
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	private void trayicon() {
		// pokud je tray ikon podporovana
		if (SystemTray.isSupported()) {
			// PopUp menu pro tray ikonu
			PopupMenu popup = new PopupMenu();

			MenuItem stop = new MenuItem("Zastavit úlohu");
			stop.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					StopActualJob();
				}
			});

			// MenuItem pro ukonceni programu
			MenuItem exit = new MenuItem("Konec");
			// action listener pro konec programu
			exit.addActionListener(exitListener);

			// pridame menuItems do PopUp menu
			popup.add(stop);
			popup.addSeparator();
			popup.add(exit);

			// obrazek pro tray ikony jako Image
			Image im = Toolkit.getDefaultToolkit().getImage("Data\\time.png");
			im = im.getScaledInstance(16, 16, Image.SCALE_SMOOTH);

			// vytvorime novou tray ikonu
			TrayIcon trayIcon = new TrayIcon(im, "Poèítaè", popup);

			// vytvorime novou systemTray
			SystemTray tray = SystemTray.getSystemTray();

			try {
				// a pridame ji do SystemTray
				tray.add(trayIcon);
			} catch (AWTException ex) {
				this.statusMessageLabel
						.setText("Chyba pri komunikaci se System Tryem: "
								+ ex.getMessage());
				Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
						null, ex);
			} catch (Exception ex) {
				this.statusMessageLabel.setText("Všeobecná chyba: "
						+ ex.getMessage());
				Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
						null, ex);
			}

			// Mouse Listener pro TrayIcon
			trayIcon.addMouseListener(new MouseListener() {
				// po kliknuti

				public void mouseClicked(MouseEvent arg0) {
					// pokud klidneme levym tlacitkem, zmeni se hodnota Visible
					if (arg0.getButton() == 1) {
						setvisible();
					}
				}

				public void mouseEntered(MouseEvent arg0) {
				}

				public void mouseExited(MouseEvent arg0) {
				}

				public void mousePressed(MouseEvent arg0) {
				}

				public void mouseReleased(MouseEvent arg0) {
				}
			});
		} else {
			JOptionPane
					.showMessageDialog(
							null,
							"Oznamovací oblast není dostupná.\nNebude dostupná ikona v této oblasti.",
							"Nefunkèní oznamovací oblast",
							JOptionPane.WARNING_MESSAGE);
		}

	}

	private void setvisible() {
		// nastavi opacnou Visiblitu nez je aktualni
		if (this.isVisible() == true) {
			this.setVisible(false);
		} else {
			this.setVisible(true);
		}
	}

	private void setColumnWidth(int kratky, int stredni, int dlouhy) {
		TableColumn column = null;

		for (int i = 0; i < this.Table.getColumnCount(); i++) {
			column = this.Table.getColumnModel().getColumn(i);
			if (i == 0) {
				column.setMinWidth(kratky);
				column.setMaxWidth(kratky);
				column.setPreferredWidth(kratky);
			} else if (i == 1 || i == 3) {
				column.setMinWidth(dlouhy);
				column.setMaxWidth(dlouhy);
				column.setPreferredWidth(dlouhy);
			} else if (i == 4 || i == 5 || i == 6) {
				column.setMinWidth(stredni);
				column.setMaxWidth(stredni);
				column.setPreferredWidth(stredni);
			}
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	private void initComponents() {

		mainPanel = new javax.swing.JPanel();
		BelowPanel = new javax.swing.JPanel();
		TlacitkovyPanel = new javax.swing.JPanel();
		InsertStartButton = new javax.swing.JButton();
		InsertButton = new javax.swing.JButton();
		StartButton = new javax.swing.JButton();
		StopButton = new javax.swing.JButton();
		DellButton = new javax.swing.JButton();
		FinishButton = new javax.swing.JButton();
		VyberFirmuPanel = new javax.swing.JPanel();
		FirmaLabel = new javax.swing.JLabel();
		FirmaComboBox = new javax.swing.JComboBox();
		PopisLabel = new javax.swing.JLabel();
		PopisTextField = new javax.swing.JTextField();
		FakturovatLabel = new javax.swing.JLabel();
		FakturovatCheckBox = new javax.swing.JCheckBox();
		TableScrollPane = new javax.swing.JScrollPane();
		Table = new javax.swing.JTable();
		AbovePanel = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		FilterFirmaTextField = new javax.swing.JTextField();
		FilterFirmaTextField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				SearchButtonActionPerformed(evt);
			}
		});
		jLabel4 = new javax.swing.JLabel();
		FilterPopisTextField = new javax.swing.JTextField();
		FilterPopisTextField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				SearchButtonActionPerformed(evt);
			}
		});
		jLabel5 = new javax.swing.JLabel();
		DatumOdTextField = new javax.swing.JTextField();
		jLabel6 = new javax.swing.JLabel();
		DatumDoTextField = new javax.swing.JTextField();
		SearchButton = new javax.swing.JButton();
		statusPanel = new javax.swing.JPanel();
		javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
		statusMessageLabel = new javax.swing.JLabel();
		statusAnimationLabel = new javax.swing.JLabel();
		progressBar = new javax.swing.JProgressBar();
		menuBar = new javax.swing.JMenuBar();
		javax.swing.JMenu fileMenu = new javax.swing.JMenu();
		exitMenuItem = new javax.swing.JMenuItem();
		viewMenu = new javax.swing.JMenu();
		RefreshMenuItem = new javax.swing.JMenuItem();
		jSeparator1 = new javax.swing.JPopupMenu.Separator();
		AktivniMenuItem = new javax.swing.JMenuItem();
		ArchivMenuItem = new javax.swing.JMenuItem();

		setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		setTitle("Sledovaè èasu v. " + PocitacApp.version);
		setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				windowClosingAdapter(evt);
			}
		});

		mainPanel.setMaximumSize(new java.awt.Dimension(400, 300));
		mainPanel.setMinimumSize(new java.awt.Dimension(400, 300));
		mainPanel.setPreferredSize(new java.awt.Dimension(400, 300));
		mainPanel.setLayout(new java.awt.BorderLayout(2, 2));

		BelowPanel.setLayout(new java.awt.BorderLayout());

		InsertStartButton.setText("Vložit + Start");
		InsertStartButton.setCursor(new java.awt.Cursor(
				java.awt.Cursor.DEFAULT_CURSOR));
		InsertStartButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						InsertStartButtonActionPerformed(evt);
					}
				});
		TlacitkovyPanel.add(InsertStartButton);

		InsertButton.setText("Vložit");
		InsertButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				InsertButtonActionPerformed(evt);
			}
		});
		TlacitkovyPanel.add(InsertButton);

		StartButton.setText("Start");
		StartButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				StartButtonActionPerformed(evt);
			}
		});
		TlacitkovyPanel.add(StartButton);

		StopButton.setText("Stop");
		StopButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				StopButtonActionPerformed(evt);
			}
		});
		TlacitkovyPanel.add(StopButton);

		DellButton.setText("Smazat");
		DellButton.setPreferredSize(new java.awt.Dimension(70, 23));
		DellButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				DellButtonActionPerformed(evt);
			}
		});
		TlacitkovyPanel.add(DellButton);

		FinishButton.setText("Dokonèit");
		FinishButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				FinishButtonActionPerformed(evt);
			}
		});
		TlacitkovyPanel.add(FinishButton);

		BelowPanel.add(TlacitkovyPanel, java.awt.BorderLayout.EAST);

		VyberFirmuPanel.setLayout(new java.awt.FlowLayout(
				java.awt.FlowLayout.LEFT));

		FirmaLabel.setText("Firma:");
		VyberFirmuPanel.add(FirmaLabel);

		FirmaComboBox.setModel(new javax.swing.DefaultComboBoxModel(
				PocitacApp.firmy));
		FirmaComboBox.setToolTipText("Vyber firmu");
		FirmaComboBox.setPreferredSize(new java.awt.Dimension(150, 20));
		VyberFirmuPanel.add(FirmaComboBox);

		PopisLabel.setText("Popis:");
		VyberFirmuPanel.add(PopisLabel);

		PopisTextField.setPreferredSize(new java.awt.Dimension(200, 20));
		VyberFirmuPanel.add(PopisTextField);

		FakturovatLabel.setText("FA:");
		VyberFirmuPanel.add(FakturovatLabel);
		VyberFirmuPanel.add(FakturovatCheckBox);

		BelowPanel.add(VyberFirmuPanel, java.awt.BorderLayout.WEST);

		mainPanel.add(BelowPanel, java.awt.BorderLayout.PAGE_END);

		Table.setAutoCreateRowSorter(true);
		Table.setModel(model);
		Table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
		TableScrollPane.setViewportView(Table);

		mainPanel.add(TableScrollPane, java.awt.BorderLayout.CENTER);

		AbovePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

		jLabel1.setText("Firma:");
		AbovePanel.add(jLabel1);

		FilterFirmaTextField.setCursor(new java.awt.Cursor(
				java.awt.Cursor.TEXT_CURSOR));
		FilterFirmaTextField.setMinimumSize(new java.awt.Dimension(100, 20));
		FilterFirmaTextField.setPreferredSize(new java.awt.Dimension(200, 20));
		AbovePanel.add(FilterFirmaTextField);

		jLabel4.setText("Popis:");
		AbovePanel.add(jLabel4);

		FilterPopisTextField.setPreferredSize(new java.awt.Dimension(200, 20));
		AbovePanel.add(FilterPopisTextField);

		jLabel5.setText("Od:");
		AbovePanel.add(jLabel5);

		DatumOdTextField.setPreferredSize(new java.awt.Dimension(50, 20));
		AbovePanel.add(DatumOdTextField);

		jLabel6.setText("Do:");
		AbovePanel.add(jLabel6);

		DatumDoTextField.setPreferredSize(new java.awt.Dimension(50, 20));
		AbovePanel.add(DatumDoTextField);

		SearchButton.setText("Hledej");
		SearchButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				SearchButtonActionPerformed(evt);
			}
		});
		AbovePanel.add(SearchButton);

		mainPanel.add(AbovePanel, java.awt.BorderLayout.NORTH);
		
		btnZrusFiltr = new JButton();
		btnZrusFiltr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FilterFirmaTextField.setText("");
				FilterPopisTextField.setText("");
				RefreshTable(model, STAV);
			}
		});
		btnZrusFiltr.setText("Zru\u0161 filtr");
		AbovePanel.add(btnZrusFiltr);

		getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

		statusMessageLabel.setForeground(new java.awt.Color(255, 0, 51));

		statusAnimationLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);

		javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(
				statusPanel);
		statusPanel.setLayout(statusPanelLayout);
		statusPanelLayout
				.setHorizontalGroup(statusPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(statusPanelSeparator,
								javax.swing.GroupLayout.DEFAULT_SIZE, 846,
								Short.MAX_VALUE)
						.addGroup(
								statusPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(statusMessageLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED,
												676, Short.MAX_VALUE)
										.addComponent(
												progressBar,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(statusAnimationLabel)
										.addContainerGap()));
		statusPanelLayout
				.setVerticalGroup(statusPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								statusPanelLayout
										.createSequentialGroup()
										.addComponent(
												statusPanelSeparator,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												2,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.addGroup(
												statusPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																statusMessageLabel)
														.addComponent(
																statusAnimationLabel)
														.addComponent(
																progressBar,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addGap(3, 3, 3)));

		getContentPane().add(statusPanel, java.awt.BorderLayout.SOUTH);

		menuBar.setAlignmentX(1.0F);
		menuBar.setAlignmentY(1.0F);
		menuBar.setPreferredSize(new java.awt.Dimension(98, 25));

		fileMenu.setText("Soubor");

		exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_Q,
				java.awt.event.InputEvent.CTRL_MASK));
		exitMenuItem.setText("Konec");
		exitMenuItem.addActionListener(exitListener);
		fileMenu.add(exitMenuItem);

		menuBar.add(fileMenu);

		viewMenu.setText("Zobrazit");

		RefreshMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_R,
				java.awt.event.InputEvent.CTRL_MASK));
		RefreshMenuItem.setText("Refresh");
		RefreshMenuItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				RefreshMenuItemActionPerformed(evt);
			}
		});
		viewMenu.add(RefreshMenuItem);
		viewMenu.add(jSeparator1);

		AktivniMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_P,
				java.awt.event.InputEvent.CTRL_MASK));
		AktivniMenuItem.setText("Poèítaè");
		AktivniMenuItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				AktivniMenuItemActionPerformed(evt);
			}
		});
		viewMenu.add(AktivniMenuItem);

		ArchivMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_A,
				java.awt.event.InputEvent.CTRL_MASK));
		ArchivMenuItem.setText("Archiv");
		ArchivMenuItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ArchivMenuItemActionPerformed(evt);
			}
		});
		viewMenu.add(ArchivMenuItem);

		menuBar.add(viewMenu);

		setJMenuBar(menuBar);
		
		mnNapoveda = new JMenu("N\u00E1pov\u011Bda");
		menuBar.add(mnNapoveda);
		
		mntmOAplikaci = new JMenuItem("O Aplikaci");
		mnNapoveda.add(mntmOAplikaci);

		pack();
	}// </editor-fold>

	private void RefreshMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
		RefreshTable(model, STAV);
	}

	private void AktivniMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
		STAV = AKTIVNI;
		RefreshTable(model, STAV);
		setTlacitkovyPanel(true);
	}

	private void ArchivMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
		STAV = ARCHIV;
		RefreshTable(model, STAV);
		setTlacitkovyPanel(false);
	}

	private void InsertButtonActionPerformed(java.awt.event.ActionEvent evt) {
		try {
			PreparedStatement pst = PocitacApp.con
					.prepareStatement("INSERT INTO Ukoly "
							+ "(Firma, Popis, Fakturovat) VALUES (?, ?, ?)");

			int fakt = 0;
			if (this.FakturovatCheckBox.isSelected()) {
				fakt = 1;
			}

			pst.setString(1, this.FirmaComboBox.getSelectedItem().toString());
			pst.setString(2, this.PopisTextField.getText());
			pst.setInt(3, fakt);

			this.statusMessageLabel.setText("Pridana nova uloha pro "
					+ this.FirmaComboBox.getSelectedItem().toString());

			pst.executeUpdate();

			RefreshTable(model, STAV);

		} catch (SQLException ex) {
			this.statusMessageLabel.setText("Chyba pøi komunikaci SQL: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (Exception ex) {
			this.statusMessageLabel.setText("Všeobecná chyba: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	private void InsertStartButtonActionPerformed(java.awt.event.ActionEvent evt) {
		try {
			PreparedStatement pst = PocitacApp.con
					.prepareStatement("INSERT INTO Ukoly "
							+ "(Firma, Popis, KdySpusteno, Spusteno, Fakturovat) VALUES (?, ?, ?, ?, ?)");

			int fakt = 0;
			if (this.FakturovatCheckBox.isSelected()) {
				fakt = 1;
			}

			pst.setString(1, this.FirmaComboBox.getSelectedItem().toString());
			pst.setString(2, this.PopisTextField.getText());
			pst.setString(3, now());
			pst.setInt(4, SPUSTENO);
			pst.setInt(5, fakt);

			this.statusMessageLabel.setText("Pridana nova uloha pro "
					+ this.FirmaComboBox.getSelectedItem().toString());

			StopActualJob();

			pst.executeUpdate();

			RefreshTable(model, STAV);

		} catch (SQLException ex) {
			this.statusMessageLabel.setText("Chyba pøi komunikaci SQL: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (ParseException ex) {
			this.statusMessageLabel.setText("Chyba parsování èasu: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (Exception ex) {
			this.statusMessageLabel.setText("Všeobecná chyba: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	private void StartButtonActionPerformed(java.awt.event.ActionEvent evt) {
		try {
			PreparedStatement pst = PocitacApp.con
					.prepareStatement("UPDATE Ukoly SET KdySpusteno = (?), Spusteno = (?) WHERE ID = (?)");

			pst.setString(1, now());
			pst.setInt(2, SPUSTENO);
			pst.setString(3, Table.getValueAt(Table.getSelectedRow(), 0)
					.toString());

			this.statusMessageLabel.setText("Do úlohy "
					+ Table.getValueAt(Table.getSelectedRow(), 0)
					+ ". vložen datum a èas: " + now());

			StopActualJob();

			pst.executeUpdate();

			RefreshTable(model, STAV);

		} catch (SQLException ex) {
			this.statusMessageLabel.setText("Chyba pøi komunikaci SQL: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (ParseException ex) {
			this.statusMessageLabel.setText("Chyba parsování èasu: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (IndexOutOfBoundsException ex) {
			this.statusMessageLabel.setText("Tahle úloha nepùjde spustit: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (Exception ex) {
			this.statusMessageLabel.setText("Všeobecná chyba: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	private void StopButtonActionPerformed(java.awt.event.ActionEvent evt) {
		StopActualJob();
	}

	private void DellButtonActionPerformed(java.awt.event.ActionEvent evt) {
		try {
			PreparedStatement pst = PocitacApp.con
					.prepareStatement("DELETE FROM Ukoly WHERE ID = (?)");

			pst.setString(1, Table.getValueAt(Table.getSelectedRow(), 0)
					.toString());

			this.statusMessageLabel.setText("Byl smazan zaznam: "
					+ Table.getValueAt(Table.getSelectedRow(), 0));

			pst.executeUpdate();

			RefreshTable(model, STAV);

		} catch (SQLException ex) {
			this.statusMessageLabel.setText("Chyba pøi komunikaci SQL: "
					+ ex.getMessage());
		} catch (ArrayIndexOutOfBoundsException ex) {
			this.statusMessageLabel
					.setText("Pokus o smazání neplatného øádku: "
							+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (IndexOutOfBoundsException ex) {
			this.statusMessageLabel.setText("Tahle úloha nepùjde spustit: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (Exception ex) {
			this.statusMessageLabel.setText("Všeobecná chyba: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	private void FinishButtonActionPerformed(java.awt.event.ActionEvent evt) {
		try {

			PreparedStatement pst = PocitacApp.con
					.prepareStatement("UPDATE Ukoly SET Dokonceno = (?) WHERE ID = (?)");

			pst.setInt(1, ARCHIV);
			pst.setString(2, Table.getValueAt(Table.getSelectedRow(), 0)
					.toString());

			this.statusMessageLabel.setText("Dokonèena úloha: "
					+ Table.getValueAt(Table.getSelectedRow(), 0).toString());

			pst.executeUpdate();

			RefreshTable(model, STAV);

		} catch (SQLException ex) {
			// this.LogTextArea.setText(ex.getMessage() + "\n" +
			// this.LogTextArea.getText());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (IndexOutOfBoundsException ex) {
			this.statusMessageLabel.setText("Tahle úloha nepùjde dokonèit: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (Exception ex) {
			this.statusMessageLabel.setText("Všeobecná chyba: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	private void SearchButtonActionPerformed(java.awt.event.ActionEvent evt) {
		try {
			PreparedStatement pst = PocitacApp.con
					.prepareStatement("SELECT * FROM Ukoly WHERE Firma like (?) AND Popis like (?) AND Dokonceno like (?) ORDER BY ID");

			pst.setString(1, "%" + this.FilterFirmaTextField.getText() + "%");
			pst.setString(2, "%" + this.FilterPopisTextField.getText() + "%");
			pst.setString(3, "" + STAV);

			PocitacApp.rs = pst.executeQuery();

			DellTable(model);

			int i = 0;
			boolean spusteno = false;
			boolean fakturovat = false;

			while (PocitacApp.rs.next()) {

				// podle 0 a 1 se rozlisi na false a true
				if (PocitacApp.rs.getString("Spusteno").equals("1")) {
					spusteno = true;
				} else {
					spusteno = false;
				}
				if (PocitacApp.rs.getString("Fakturovat").equals("1")) {
					fakturovat = true;
				} else {
					fakturovat = false;
				}

				model.addRow(new Object[] { PocitacApp.rs.getString("ID"),
						PocitacApp.rs.getString("Firma"),
						PocitacApp.rs.getString("Popis"),
						PocitacApp.rs.getString("KdySpusteno"),
						PocitacApp.rs.getString("Cas"), spusteno, fakturovat });
				i++;
			}

			this.statusMessageLabel.setText("Vyhledáno " + i + " záznamù");

		} catch (SQLException ex) {
			this.statusMessageLabel.setText("Chyba pøi komunikaci SQL: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (Exception ex) {
			this.statusMessageLabel.setText("Všeobecná chyba: "
					+ ex.getMessage());
			Logger.getLogger(PocitacView.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	ActionListener exitListener = new java.awt.event.ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent evt) {
			if (Spusteno()) {
				int close = JOptionPane
						.showConfirmDialog(
								null,
								"Je spuštìná nìjaká úloha.\nChcete ji pøed zavøením zastavit?",
								"Poèítaè bìží",
								JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.INFORMATION_MESSAGE);
				switch (close) {
				case JOptionPane.YES_OPTION:
					StopActualJob();
					Konec();
				case JOptionPane.NO_OPTION: // Konec zvonec
					Konec();
				case JOptionPane.CANCEL_OPTION: // nedelej nic
					break;
				default: // nedelej nic
					break;
				}
			} else {
				Konec();
			}
		}
	};

	private void windowClosingAdapter(WindowEvent evt) {
		if (Spusteno()) {
			int close = JOptionPane
					.showConfirmDialog(
							null,
							"Je spuštìná nìjaká úloha.\nChcete ji pøed zavøením zastavit?",
							"Poèítaè bìží", JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.INFORMATION_MESSAGE);
			switch (close) {
			case JOptionPane.YES_OPTION:
				StopActualJob();
				Konec();
			case JOptionPane.NO_OPTION: // Konec zvonec
				Konec();
			case JOptionPane.CANCEL_OPTION: // nedelej nic
				break;
			default: // nedelej nic
				break;
			}
		} else {
			Konec();
		}
	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(new Runnable() {

			public void run() {
				new PocitacView().setVisible(true);
			}
		});
	}

	// Variables declaration - do not modify
	private javax.swing.JPanel AbovePanel;
	private javax.swing.JMenuItem AktivniMenuItem;
	private javax.swing.JMenuItem ArchivMenuItem;
	private javax.swing.JPanel BelowPanel;
	private javax.swing.JTextField DatumDoTextField;
	private javax.swing.JTextField DatumOdTextField;
	private javax.swing.JButton DellButton;
	private javax.swing.JCheckBox FakturovatCheckBox;
	private javax.swing.JLabel FakturovatLabel;
	private javax.swing.JTextField FilterFirmaTextField;
	private javax.swing.JTextField FilterPopisTextField;
	private javax.swing.JButton FinishButton;
	private javax.swing.JComboBox FirmaComboBox;
	private javax.swing.JLabel FirmaLabel;
	private javax.swing.JButton InsertButton;
	private javax.swing.JButton InsertStartButton;
	private javax.swing.JLabel PopisLabel;
	private javax.swing.JTextField PopisTextField;
	private javax.swing.JMenuItem RefreshMenuItem;
	private javax.swing.JButton SearchButton;
	private javax.swing.JButton StartButton;
	private javax.swing.JButton StopButton;
	private javax.swing.JTable Table;
	private javax.swing.JScrollPane TableScrollPane;
	private javax.swing.JPanel TlacitkovyPanel;
	private javax.swing.JPanel VyberFirmuPanel;
	private javax.swing.JMenuItem exitMenuItem;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JPopupMenu.Separator jSeparator1;
	private javax.swing.JPanel mainPanel;
	private javax.swing.JMenuBar menuBar;
	private javax.swing.JProgressBar progressBar;
	private javax.swing.JLabel statusAnimationLabel;
	private javax.swing.JLabel statusMessageLabel;
	private javax.swing.JPanel statusPanel;
	private javax.swing.JMenu viewMenu;
	private JButton btnZrusFiltr;
	private JMenu mnNapoveda;
	private JMenuItem mntmOAplikaci;
	// End of variables declaration
}

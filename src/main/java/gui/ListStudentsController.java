
package gui;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;

import application.Main;
import db.DBFactory;
import db.DbException;
import gui.util.Alerts;
import gui.util.FxmlPaths;
import gui.util.Icons;
import gui.util.Utils;
import gui.util.enums.ParcelStatusEnum;
import gui.util.enums.StudentStatusEnum;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.dao.AnnotationDao;
import model.dao.StudentDao;
import model.entites.Annotation;
import model.entites.Matriculation;
import model.entites.Parcel;
import model.entites.Student;

public class ListStudentsController implements Initializable {

	// Filter Student and Register
	@FXML JFXTextField textFilter;
	@FXML JFXComboBox<String> comboBoxfiledFilter;
	@FXML ToggleGroup filterType;
	@FXML ToggleGroup StudentStatus;
	@FXML JFXButton btnRegister;
	// Table Students
	@FXML TableView<Student> tableStudents;
	@FXML TableColumn<Student, String> columnStudentStatus;
	@FXML TableColumn<Student, Integer> columnStudentCode;
	@FXML TableColumn<Student, String> columnStudentName;
	@FXML TableColumn<Student, String> columnStudentContact1;
	@FXML TableColumn<Student, Student> columnStudentInfo;
	// Table Matriculations
	@FXML TableView<Matriculation> tableMatriculations;
	@FXML TableColumn<Matriculation, Integer> columnMatriculationCode;
	@FXML TableColumn<Matriculation, Date> columnMatriculationDate;
	@FXML TableColumn<Matriculation, String> columnMatriculationStatus;
	@FXML TableColumn<Matriculation, String> columnMatriculationParcels;
	// Table Parcels
	@FXML Label labelSelectedMatriculation;
	@FXML TableView<Parcel> tableParcels;
	@FXML TableColumn<Parcel, String> columnParcelStatus;
	@FXML TableColumn<Parcel, Integer> columnParcelParcel;
	@FXML TableColumn<Parcel, Date> columnParcelDate;
	@FXML TableColumn<Parcel, Double> columnParcelValue;
	// Annotations
	@FXML Button btnAddAnnotation;
	@FXML JFXListView<Annotation> listViewAnnotation;
	@FXML Label labelSelectedAnnotationDate;
	@FXML JFXTextArea textAreaAnnotation;
	@FXML Button btnEditSelectedAnnotation;
	@FXML Button btnDeleteSelectedAnnottion;
	@FXML Label labelSelectedAnnotationCollaborator;
	
	private StudentDao studentDao;
	private ObservableList<Student> studentsList;
	private MainViewController mainView;	
	
	private final Integer ICON_SIZE = 15;
	
	@Override
	public void initialize(URL url, ResourceBundle resources) {
		initializeTableStudentsNodes();
		initializeTableMatriculationsNodes();
		initiliazeTableParcelsNodes();
		initiliazeListViewAnnotations();
	}
	
	public void setMainViewController(MainViewController mainView) {
		this.mainView = mainView;
	}
	
	public void setStudentDao(StudentDao studentDao) {
		this.studentDao = studentDao;
		try {
			studentsList = FXCollections.observableArrayList(this.studentDao.findAllWithContactsLoaded());
			tableStudents.setItems(studentsList);
			tableStudents.getSelectionModel().selectFirst();
		} catch (DbException e) {
			Alerts.showAlert("Erro ao carregar os alunos", "DBException", e.getMessage(), AlertType.ERROR);
		}
	}
	
	private void initializeTableStudentsNodes() {
		Utils.setCellValueFactory(columnStudentStatus, "status");
		columnStudentStatus.setCellFactory(column -> {
			return new TableCell<Student, String>() {
				@Override
				protected void updateItem(String status, boolean empty) {
					super.updateItem(status, empty);
					setText("");
					setGraphic(null);
					if (!isEmpty() && status != null) {
						this.setStyle("-fx-background-color:" + StudentStatusEnum.fromString(getItem()).getHexColor());
					}
				}
			};
	    });
		Utils.setCellValueFactory(columnStudentCode, "id");
		Utils.setCellValueFactory(columnStudentName, "name");
		columnStudentContact1.setCellValueFactory(cellData -> {
			try {
				return new SimpleStringProperty(cellData.getValue().getContacts().get(0).getNumber());
			}catch(IllegalStateException | IndexOutOfBoundsException e) {
				return new SimpleStringProperty("-");
			}
		});
		// Info button
		Utils.initButtons(columnStudentInfo, ICON_SIZE, Icons.INFO_CIRCLE_SOLID, "grayIcon", (student, event) -> {
			showStudentInfo(student, FxmlPaths.INFO_STUDENT);
		});
		// Listener to selected student
		tableStudents.getSelectionModel().selectedItemProperty().addListener(
	            (observable, oldSelection, newSelection) -> {
					tableMatriculations.setItems(null);
					if (newSelection != null) {
						updateAnnotations(null);
						if (newSelection.getMatriculations().size() > 0) {
							try {
								tableMatriculations
										.setItems(FXCollections.observableList(newSelection.getMatriculations()));
								tableMatriculations.getSelectionModel().selectFirst();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
	    );
	}
	
	private void initializeTableMatriculationsNodes() {
		Utils.setCellValueFactory(columnMatriculationCode, "code");
		Utils.setCellValueFactory(columnMatriculationDate, "dateMatriculation");
		Utils.formatTableColumnDate(columnMatriculationDate, "dd/MM/yyyy");
		Utils.setCellValueFactory(columnMatriculationStatus, "status");
		columnMatriculationParcels.setCellValueFactory(cellData -> {
			try {
				// Total of parcels ignoring matriculation tax (parcel 0)
				List<Parcel> parcels = cellData.getValue().getParcels().stream()
						.filter(parcel -> parcel.getParcelNumber() != 0).collect(Collectors.toList());
				int paidParcels = parcels.stream().filter(parcel -> parcel.getSituation()
						.equalsIgnoreCase("PAGO")).collect(Collectors.toList()).size();
				return new SimpleStringProperty(paidParcels + "/" + parcels.size());
			}catch(IllegalStateException | IndexOutOfBoundsException e) {
				return new SimpleStringProperty("-");
			}
		});
		// Listener to selected Matriculation
		tableMatriculations.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldSelection, newSelection) -> {					
					tableParcels.setItems(null);
					tableParcels.refresh();
					labelSelectedMatriculation.setText("");
					if (newSelection != null && newSelection.getParcels().size() > 0) {
						try {
							tableParcels.setItems(FXCollections.observableList(newSelection.getParcels()));
							String matriculationCode = Integer.toString(newSelection.getCode());
							setCurrentMatriculationId(matriculationCode);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
	}
	
	private void initiliazeTableParcelsNodes() {		
		// Status Color
		columnParcelStatus.setCellValueFactory(cellData -> {
			try {
				String situation = cellData.getValue().getSituation();
				Parcel auxParcel = cellData.getValue();
				if (auxParcel.getDateParcel().before(new Date()) && auxParcel.getSituation().equalsIgnoreCase("ABERTA")) {
					situation = "ATRASADA";
				}
				return new SimpleStringProperty(situation);
			}catch(IllegalStateException | IndexOutOfBoundsException e) {
				return new SimpleStringProperty("");
			}
		});
		columnParcelStatus.setCellFactory(column -> {
			return new TableCell<Parcel, String>() {
				@Override
				protected void updateItem(String situation, boolean empty) {
					super.updateItem(situation, empty);
					setText("");
					setGraphic(null);
					if (!isEmpty()) {
						this.setStyle("-fx-background-color:" + ParcelStatusEnum.fromString(getItem()).getHexColor());
					}
				}
			};
	    });
		Utils.setCellValueFactory(columnParcelParcel, "parcelNumber");
		Utils.setCellValueFactory(columnParcelDate, "dateParcel");
		Utils.formatTableColumnDate(columnParcelDate, "dd/MM/yyyy");
		Utils.setCellValueFactory(columnParcelValue, "value");
		Utils.formatTableColumnDoubleCurrency(columnParcelValue);
	}
	
	private void initiliazeListViewAnnotations() {
		listViewAnnotation.setCellFactory(param -> new ListCell<Annotation>() {
		    @Override
		    protected void updateItem(Annotation item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setGraphic(null);
				} else {
					// Create the HBox
					HBox hBox = new HBox();
					hBox.setAlignment(Pos.CENTER);
					// Create centered Label
					SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
					Label label = new Label(sdf.format(item.getDate()));
		            label.setAlignment(Pos.CENTER);
		            hBox.getChildren().add(label);
					setGraphic(hBox);
				}
			}
		});
		// Listener to selected Annotation
		listViewAnnotation.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldSelection, newSelection) -> {
					try {
						labelSelectedAnnotationDate.setText("");
						textAreaAnnotation.setText("");
						labelSelectedAnnotationCollaborator.setText("");
						tableParcels.refresh();
						if (newSelection != null) {
							SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
							labelSelectedAnnotationDate.setText(sdf.format(newSelection.getDate()));
							textAreaAnnotation.setText(newSelection.getDescription());
							labelSelectedAnnotationCollaborator.setText(newSelection.getResponsibleCollaborator());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
	}
	
	public void handleBtnAddNewStudent(ActionEvent event) {
		System.out.println("You will matriculate a new person");
	}
	
	public void handleBtnAddAnnotation(ActionEvent event) {
		Student studentSelected = tableStudents.getSelectionModel().getSelectedItem();
		if (studentSelected != null) {
			loadView(FxmlPaths.ANNOTATION, Utils.currentStage(event), "Adicionar Anota��o", false, (controller) -> {
				AnnotationController annotationController = (AnnotationController) controller;
				annotationController.setAnnotationDao(new AnnotationDao(DBFactory.getConnection()));
				annotationController.setDependences(null, studentSelected, Main.getCurrentUser().getName(), this);
			});
		}
	}
	
	public void handleBtnEditAnnotation(ActionEvent event){
		Annotation itemSelected = listViewAnnotation.getSelectionModel().getSelectedItem();
		if(itemSelected != null) {
			loadView(FxmlPaths.ANNOTATION, Utils.currentStage(event), "Editar Anota��o", false, (controller) -> {
				AnnotationController annotationController = (AnnotationController) controller;
				annotationController.setAnnotationDao(new AnnotationDao(DBFactory.getConnection()));
				annotationController.setDependences(itemSelected, Main.getCurrentUser().getName(), this);
				
			});
		}
	}
	
	public void handleBtnDeleteAnnotation(ActionEvent event){
		Annotation itemSelected = listViewAnnotation.getSelectionModel().getSelectedItem();
		if(itemSelected != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - HH:mm");
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Deletar anota��o");
			alert.setHeaderText("Deletar a anota��o do dia " + sdf.format(itemSelected.getDate()) + " ?");
			Optional<ButtonType> result =alert.showAndWait();
			if (result.isPresent() && result.get() == ButtonType.OK) {
				AnnotationDao annotationDao = new AnnotationDao(DBFactory.getConnection());
				try {
					annotationDao.delete(itemSelected);
					itemSelected.getStudent().getAnnotations().remove(itemSelected);
					updateAnnotations(null);
				} catch (DbException e) {
					Alerts.showAlert("Erro ao deletar anota��o", "DbException", e.getMessage(), AlertType.ERROR);
				}
			}
		}
	}
	
	private void showStudentInfo(Student student, String FxmlPath) {
		try {
			
			mainView.setContent(FxmlPath, (InfoStudentController controller) -> {
				controller.setMainViewControllerAndReturnName(mainView, "Alunos");
				controller.setCurrentStudent(student, FxmlPaths.LIST_STUDENTS);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setCurrentMatriculationId(String matriculationCode) {
		labelSelectedMatriculation.setText("Matr�cula: " + matriculationCode);
	}
	
	public void updateAnnotations(Annotation annotation) {
		listViewAnnotation.setItems(null);
		Student studentSelected = tableStudents.getSelectionModel().getSelectedItem();
		try {
			ObservableList<Annotation> annotations = FXCollections.observableList(studentSelected.getAnnotations());
			annotations.sort((a1, a2) -> a2.getDate().compareTo(a1.getDate()));
			listViewAnnotation.setItems(annotations);
			if (annotation != null) {
				listViewAnnotation.getSelectionModel().select(annotation);
			} else {
				listViewAnnotation.getSelectionModel().selectFirst();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private synchronized <T> void loadView(String FXMLPath, Stage parentStage, String windowTitle,
			boolean resizable, Consumer<T> initializingAction) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource(FXMLPath));
			Parent parent = loader.load();
			Stage dialogStage = new Stage();
			dialogStage.setTitle(windowTitle);
			dialogStage.initModality(Modality.WINDOW_MODAL);
			dialogStage.initOwner(parentStage);
			dialogStage.setResizable(resizable);
			
			Scene scene = new Scene(parent);
			scene.getStylesheets().add(getClass().getResource("/application/application.css").toExternalForm());			
			dialogStage.setScene(scene);
			
			T controller = loader.getController();
			initializingAction.accept(controller);
			dialogStage.showAndWait();
		} catch (IOException e) {
			e.printStackTrace();
			Alerts.showAlert("IOException", "Erro ao exibir tela", e.getMessage(), AlertType.ERROR);
		} catch(IllegalStateException e) {
			Alerts.showAlert("IllegalStateException", "Erro ao exibir tela", e.getMessage(), AlertType.ERROR);
		}
	}	

}

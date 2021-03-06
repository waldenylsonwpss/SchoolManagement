package model.entites;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity(name = "Matricula")
@Table(name = "matricula")
public class Matriculation {
	
	@EqualsAndHashCode.Include
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer code;
	
	@ManyToOne
	@JoinColumn(name="aluno_id")
	private Student student;
	
	@ManyToOne
	@JoinColumn(name="responsavel_financeiro_id")
	private Responsible responsible;
	
	@Column(name="data_matricula", columnDefinition = "date default null")
	private Date dateMatriculation;

	@Column(name="matriculado_por", columnDefinition = "varchar(50) default null")
	private String matriculatedBy;
	
	@Column(name="motivo", columnDefinition = "varchar(50) default null")
	private String reason;
	
	@Column(name="servico_contratado", columnDefinition = "text default null")
	private String serviceContracted;
	
	@Column(name="situacao", columnDefinition = "varchar(50) default null")
	private String status;
	
	@OneToMany(mappedBy = "matriculation", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Parcel> parcels;


}

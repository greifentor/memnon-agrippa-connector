package de.ollie.memnon.agrippa.connector;

import de.ollie.memnon.core.model.ConnectorId;
import de.ollie.memnon.core.model.ErinnerungId;
import de.ollie.memnon.core.model.ExternalErinnerung;
import de.ollie.memnon.core.service.port.connector.ExternalErinnerungConnector;
import jakarta.inject.Named;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Named
@RequiredArgsConstructor
public class AgrippaExternalErinnerungConnectorImpl implements ExternalErinnerungConnector {

	private static final ConnectorId CONNECTOR_ID = new ConnectorId(UUID.randomUUID());

	private final AgrippaJdbcConfiguration configuration;

	public class AgrippaErinnerungId extends ErinnerungId {

		@Getter
		private long agrippaId;

		public AgrippaErinnerungId(UUID id, long agrippaId) {
			super(id);
			this.agrippaId = agrippaId;
		}
	}

	@Override
	public boolean canBeConfirmed() {
		return false;
	}

	@Override
	public boolean confirm(ErinnerungId id) {
		// TODO OLI: Think about a valid confirm strategy for todos.
		String sql = "update TODO set STATUS = ? WHERE id = ?";
		try (
			Connection conn = DriverManager.getConnection(
				configuration.getUrl(),
				configuration.getUsername(),
				configuration.getPassword()
			);
			PreparedStatement pstmt = conn.prepareStatement(sql)
		) {
			pstmt.setString(1, "SOLVED");
			pstmt.setObject(2, ((AgrippaErinnerungId) id).getAgrippaId());
			return pstmt.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public List<ExternalErinnerung> findAllErinnerungen() {
		List<ExternalErinnerung> l = new ArrayList<>();
		String stmt =
			"select t.DUE_DATE as DATE, t.ID, concat(ta.TITLE, \" - \", t.TITLE) as DESCRIPTION " + //
			"from TODO t " + //
			"left join TASK ta on ta.ID = t.TASK " + //
			"where t.DUE_DATE is not null and t.STATUS = ?";
		try (
			Connection conn = DriverManager.getConnection(
				configuration.getUrl(),
				configuration.getUsername(),
				configuration.getPassword()
			);
			PreparedStatement pstmt = conn.prepareStatement(stmt);
		) {
			pstmt.setString(1, "OPEN");
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					LocalDateTime date = rs.getObject("DATE", LocalDateTime.class);
					l.add(
						(ExternalErinnerung) new ExternalErinnerung()
							.setConnectorId(getId())
							.setBezugsdatum(date.toLocalDate())
							.setNaechsterTermin(date.toLocalDate())
							.setId(new AgrippaErinnerungId(UUID.randomUUID(), rs.getLong("ID")))
							.setName("(" + date.toLocalTime() + ")" + rs.getString("DESCRIPTION"))
							.setWiederholung(null)
					);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return l;
	}

	@Override
	public ConnectorId getId() {
		return CONNECTOR_ID;
	}
}

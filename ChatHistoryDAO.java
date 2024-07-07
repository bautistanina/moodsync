import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;



public class ChatHistoryDAO {

    public void saveChatMessage(int userId, String message, int conversationId, String timestamp) throws SQLException {
        String sql = "INSERT INTO chat_history (user_id, message, conversation_id, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, message);
            stmt.setInt(3, conversationId);
            stmt.setString(4, timestamp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public List<Integer> getUserConversationIds(int userId) throws SQLException {
        List<Integer> conversationIds = new ArrayList<>();
        String query = "SELECT DISTINCT conversation_id FROM chat_history WHERE user_id = ? ORDER BY conversation_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                conversationIds.add(rs.getInt("conversation_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return conversationIds;
    }

    public ArrayList<String> getChatHistory(int userId, int conversationId) throws SQLException {
        ArrayList<String> chatHistory = new ArrayList<>();
        String query = "SELECT message, timestamp FROM chat_history WHERE user_id = ? AND conversation_id = ? ORDER BY timestamp ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, conversationId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String message = rs.getString("message");
                String timestamp = rs.getString("timestamp");
                chatHistory.add(message + " (" + timestamp + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return chatHistory;
    }

    public void saveGeneratedTitle(int conversationId, String title) throws SQLException {
        String query = "UPDATE chat_history SET conversation_title = ? WHERE conversation_id = ? AND conversation_title IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, title);
            stmt.setInt(2, conversationId);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                // handle the case where no rows are updated
                System.out.println("No conversation found with ID: " + conversationId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public String getGeneratedTitle(int conversationId) throws SQLException {
        String query = "SELECT conversation_title FROM chat_history WHERE conversation_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, conversationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("conversation_title");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return null;
    }

    public void deleteConversation(int conversationId) throws SQLException {
        String query = "DELETE FROM chat_history WHERE conversation_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, conversationId);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted == 0) {
                // handle the case where no rows are deleted
                System.out.println("No conversation found with ID: " + conversationId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
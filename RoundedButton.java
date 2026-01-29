import javax.swing.*;
import java.awt.*;

public class RoundedButton extends JButton {

    private int radius = 20;

    public RoundedButton(String text) {
        super(text);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setForeground(Color.WHITE); // white text
        setBackground(new Color(200, 0, 0)); // red color
        setFont(new Font("Arial", Font.BOLD, 14));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill with rounded rectangle
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

        // Draw text
        super.paintComponent(g);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        // Optional: add a border outline
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Color.DARK_GRAY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
        g2.dispose();
    }
}
 
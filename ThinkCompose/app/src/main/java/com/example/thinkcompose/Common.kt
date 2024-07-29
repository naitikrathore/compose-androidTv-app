package com.example.thinkcompose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thinkcompose.Model.ThinkModel
import com.example.thinkcompose.ui.theme.primaryLight

//@Composable
//@Preview
//fun previewTool() {
//    MainToolBar(title = "Demo Tool",{
//
//    })
//}

@Composable
fun MainToolBar(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .padding(10.dp)
                .size(45.dp)
                .clickable {
                    onClick.invoke()
                },
            colors = CardDefaults.cardColors(containerColor = primaryLight),
            shape = RoundedCornerShape(200.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier
                        .padding(10.dp),
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        Text(
            modifier = Modifier
                .padding(5.dp),
            text = title,
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

fun getList():List<ThinkModel>{
    return listOf(
        ThinkModel("Technological Advancements", listOf(
            "Google achieves quantum supremacy.",
            "Worldwide rollout of 5G networks.",
            "SpaceX's Starship completes first manned Mars mission.",
            "AI predicts disease outbreaks with high accuracy.",
            "Tesla's new battery technology extends range by 50%.",
            "Solar power becomes cheaper than fossil fuels.",
            "Implementation of IoT sensors for urban management.",
            "Boston Dynamics' robots integrated into logistics.",
            "AR glasses replace smartphones.",
            "Widespread adoption of blockchain for supply chain management.",
            "CRISPR used to eliminate genetic diseases.",
            "Smart wearables monitor health metrics continuously.",
            "Hydrogen fuel cells power commercial flights.",
            "Nanotechnology in medical treatments.",
            "Edge computing enhances data processing speeds."
        )),
        ThinkModel("AI Innovations", listOf(
            "AI-driven personal assistants.",
            "Machine learning models improve climate predictions.",
            "Autonomous vehicles in public transport.",
            "AI algorithms boost cybersecurity defenses.",
            "Natural language processing in customer service.",
            "AI enhances personalized education.",
            "Deep learning models in financial forecasting.",
            "AI in automated content creation.",
            "AI-powered drug discovery.",
            "Robotic process automation in businesses.",
            "Facial recognition systems in security.",
            "AI in predictive maintenance for industries.",
            "AI-driven marketing strategies.",
            "AI in real-time language translation.",
            "AI models in weather forecasting."
        )),
        ThinkModel("Big Companies' Strategies", listOf(
            "Amazon expands into healthcare sector.",
            "Apple focuses on privacy features in products.",
            "Google invests in renewable energy.",
            "Microsoft's cloud services growth strategy.",
            "Tesla's vertical integration in production.",
            "Facebook's push into virtual reality.",
            "Netflix's original content investment.",
            "Alibaba's global e-commerce expansion.",
            "Samsung's innovation in semiconductor technology.",
            "IBM's focus on quantum computing.",
            "Intel's shift towards AI and data centers.",
            "NVIDIA's dominance in GPU market.",
            "Oracle's cloud infrastructure developments.",
            "Cisco's advancements in network security.",
            "Sony's investments in entertainment and gaming."
        )),
        ThinkModel("Business Models of Companies", listOf(
            "Subscription-based streaming services.",
            "Freemium models in software.",
            "E-commerce platforms with third-party sellers.",
            "On-demand ride-sharing services.",
            "Crowdsourced content creation platforms.",
            "Ad-supported free services.",
            "Pay-per-use cloud computing.",
            "Direct-to-consumer retail strategies.",
            "Marketplace models in digital goods.",
            "Gig economy platforms.",
            "Digital subscription news services.",
            "Data monetization in social media.",
            "Open-source software with premium support.",
            "Virtual goods in gaming.",
            "Hybrid work models in tech companies."
        )),
        ThinkModel("Future Trends in Tech", listOf(
            "Widespread adoption of electric vehicles.",
            "Growth of the Internet of Things (IoT).",
            "Advances in augmented reality and VR.",
            "Increased use of renewable energy.",
            "Rise of smart cities.",
            "Proliferation of 5G technology.",
            "Advancements in space exploration.",
            "Growth of biotech and genetic editing.",
            "Expansion of AI in everyday life.",
            "Increased focus on cybersecurity.",
            "Development of quantum computing.",
            "Growth of blockchain technology.",
            "Emergence of new social media platforms.",
            "Advancements in wearable tech.",
            "Rise of remote work technologies."
        )),
        ThinkModel("Impact of AI on Society", listOf(
            "AI in healthcare diagnostics.",
            "AI in autonomous vehicles.",
            "AI-driven job displacement.",
            "Ethical concerns in AI development.",
            "AI in financial services.",
            "AI in education and learning.",
            "AI in smart home devices.",
            "AI in environmental monitoring.",
            "AI in entertainment and media.",
            "AI in retail and e-commerce.",
            "AI in public safety.",
            "AI in personalized marketing.",
            "AI in agriculture and food production.",
            "AI in energy management.",
            "AI in human resources."
        )),
        ThinkModel("Tech-Driven Business Strategies", listOf(
            "Data-driven decision making.",
            "AI-powered customer service.",
            "Blockchain for transparent supply chains.",
            "IoT for operational efficiency.",
            "Cloud computing for scalability.",
            "Mobile-first business models.",
            "Digital transformation initiatives.",
            "Cybersecurity investments.",
            "Remote work enablement.",
            "AI for predictive analytics.",
            "Automation of routine tasks.",
            "Virtual collaboration tools.",
            "Sustainable technology solutions.",
            "Tech-driven marketing strategies.",
            "Personalized user experiences."
        )),
        ThinkModel("AI Applications in Industries", listOf(
            "AI in manufacturing for quality control.",
            "AI in healthcare for predictive analytics.",
            "AI in finance for fraud detection.",
            "AI in retail for inventory management.",
            "AI in transportation for route optimization.",
            "AI in agriculture for crop monitoring.",
            "AI in real estate for property valuation.",
            "AI in education for personalized learning.",
            "AI in entertainment for content recommendation.",
            "AI in energy for grid management.",
            "AI in insurance for risk assessment.",
            "AI in legal for document review.",
            "AI in marketing for customer segmentation.",
            "AI in human resources for talent acquisition.",
            "AI in public services for citizen engagement."
        )),
        ThinkModel("Tech Investments by Big Companies", listOf(
            "Amazon's investment in AI and robotics.",
            "Google's focus on AI research.",
            "Microsoft's push into cloud computing.",
            "Apple's advancements in health tech.",
            "Facebook's development of AR/VR.",
            "Tesla's R&D in battery technology.",
            "Alibaba's foray into AI-driven retail.",
            "Samsung's innovation in 5G technology.",
            "IBM's commitment to quantum computing.",
            "Intel's development of AI chips.",
            "NVIDIA's expansion in AI and gaming.",
            "Oracle's cloud infrastructure investment.",
            "Cisco's advancements in IoT.",
            "Sony's investment in gaming and entertainment.",
            "Netflix's focus on AI-driven content creation."
        )),
        ThinkModel("Emerging Technologies", listOf(
            "Quantum computing for problem solving.",
            "5G technology for faster connectivity.",
            "Blockchain for secure transactions.",
            "IoT for interconnected devices.",
            "AI for automated decision making.",
            "AR/VR for immersive experiences.",
            "Biotech for health advancements.",
            "Nanotech for material science.",
            "Robotics for automation.",
            "Cybersecurity for data protection.",
            "3D printing for manufacturing.",
            "Renewable energy for sustainability.",
            "Edge computing for faster processing.",
            "Autonomous vehicles for transportation.",
            "Wearable tech for personal health."
        ))
    )
}


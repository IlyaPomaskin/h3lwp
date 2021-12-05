package com.homm3.livewallpaper.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.homm3.livewallpaper.R

class SettingsComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting("Android", listOf("asd", "asdf", "zxcvasdf", "qwerqwer"))
                }
            }
        }
    }
}

data class Message(val key: Int, val author: String, val body: String)

@Composable
fun MessageCard(msg: Message) {
    // Add padding around our message
    Row(modifier = Modifier.padding(all = 8.dp)) {
        // Add a horizontal space between the image and the column
        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(text = msg.author)
            // Add a vertical space between the author and message texts
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = msg.body)
        }
    }
}

@Composable
fun Conversation(messages: List<Message>) {
    LazyColumn {
        items(messages.size, { it }) { index ->
            MessageCard(messages[index])
        }
    }
}

@Composable
fun Greeting(name: String, messages: List<String>) {

    Column() {
        Text(text = "Hello $name!")
    }

    Column() {
        LazyColumn {
            items(messages.size, { it }) { index ->
                Text(text = "msg: ${messages[index]}")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        Column(Modifier, Arrangement.Center, Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            )

            Text(
                text = "To setup Heroes 3 Live Wallpaper you will need file \"h3sprite.lod\" from the original game",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Button(onClick = { /*TODO*/ }) {
                    Text(text = "Where to find it?")
                }
            }

            Column() {
                Button(onClick = { /*TODO*/ }) {
                    Text(text = "Select h3sprites.lod")
                }
            }
        }

    }
}
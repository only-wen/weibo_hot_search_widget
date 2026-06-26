package com.example.weibohotsearchwidget.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.weibohotsearchwidget.data.DefaultDataRepository
import com.example.weibohotsearchwidget.theme.WeiboHotSearchWidgetTheme

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  context: android.content.Context = LocalContext.current.applicationContext,
  viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository(context)) },
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  
  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .safeDrawingPadding()
    ) {
      // 1. Premium Brand Header Card
      HeaderCard()

      // 2. Onboarding Guide Card
      OnboardingGuide()

      Spacer(modifier = Modifier.height(16.dp))

      // 3. Live Preview Section Header
      Text(
        text = "🔥 实时热搜数据预览",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onBackground
      )

      // 4. Live Preview List
      when (state) {
        MainScreenUiState.Loading -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(color = Color(0xFFE6162D))
          }
        }
        is MainScreenUiState.Success -> {
          val data = (state as MainScreenUiState.Success).data
          PreviewList(data = data)
        }
        is MainScreenUiState.Error -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "加载错误: ${(state as MainScreenUiState.Error).throwable.message}\n请检查网络连接",
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(16.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun HeaderCard() {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
  ) {
    Box(
      modifier = Modifier
        .background(
          Brush.horizontalGradient(
            colors = listOf(Color(0xFFE6162D), Color(0xFFFF8200))
          )
        )
        .padding(20.dp)
    ) {
      Column {
        Text(
          text = "微博热搜桌面小组件",
          color = Color.White,
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "每小时自动更新 · 支持手动刷新 · 极致省电",
          color = Color.White.copy(alpha = 0.85f),
          fontSize = 13.sp
        )
      }
    }
  }
}

@Composable
fun OnboardingGuide() {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "💡 小组件添加与安装指南",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(10.dp))
      
      val steps = listOf(
        "1. 在手机桌面空白处【长按】或双指捏合",
        "2. 选择【添加小组件】或【窗口小工具】",
        "3. 搜索或滑到最下方，找到【微博热搜】组件",
        "4. 将小组件拖放到桌面，并可自由调整大小"
      )
      
      steps.forEach { step ->
        Text(
          text = step,
          fontSize = 12.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
          modifier = Modifier.padding(vertical = 2.dp)
        )
      }
    }
  }
}

@Composable
fun PreviewList(data: List<String>) {
  val context = LocalContext.current
  
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    contentPadding = PaddingValues(bottom = 16.dp)
  ) {
    itemsIndexed(data) { index, word ->
      val rank = index + 1
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            val encodedWord = Uri.encode(word)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://s.weibo.com/weibo?q=$encodedWord"))
            context.startActivity(intent)
          }
          .background(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            shape = RoundedCornerShape(8.dp)
          )
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Rank circle background
        val badgeColor = when (rank) {
          1 -> Color(0xFFFF1D45)
          2 -> Color(0xFFFF7A00)
          3 -> Color(0xFFFCA311)
          else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        }
        val textColor = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurface

        Box(
          modifier = Modifier
            .size(20.dp)
            .background(color = badgeColor, shape = CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = rank.toString(),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Topic Title
        Text(
          text = word,
          fontSize = 13.5.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  WeiboHotSearchWidgetTheme {
    Column {
      HeaderCard()
      OnboardingGuide()
      PreviewList(listOf("热搜话题一", "热搜话题二", "热搜话题三"))
    }
  }
}

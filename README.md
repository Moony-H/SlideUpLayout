# SlideLayout

뷰를 슬라이드 하여 화면을 일부, 또는 전체를 가릴 수 있게 하는 ViewGroup을 만들었습니다.

이 뷰 그룹은 단 두개의 자식 View만을 가질 수 있으며, 모두 match_parent 속성이어야 합니다.

<br/>

**첫번째 자식 뷰는 배경을 담당하는 MainView, 두 번째 자식 뷰는 슬라이드가 되는 SlideView 입니다.**

<br/>

안드로이드에 있는 ViewGroup을 상속하여 만들었습니다.

또한 ViewDragHelper라는 클래스를 이용하였습니다.

<br/>

## 기능 1) 슬라이드



![UpDown](https://user-images.githubusercontent.com/53536205/158807595-076c402c-462a-463d-8e35-50e1350d7ee1.gif)





뷰를 잡아 끌어 뷰를 이동시킬 수 있습니다.

<br/>

SlideLayout이 사용자의 터치 속도를 계산하여 일정 범위를 넘어 서면 onInterceptTouchEvent를 발동시켜, 다음 자식 뷰 까지 터치가 넘어가지 않고 중간에 가로챕니다.

<br/>

가로챈 터치는 **SlideView**를 움직이기 위해 사용됩니다. 이 때 활용한 것이 ViewDragHelper 입니다.

<br/>

```kotlin
override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        //단 한번이라도 return true를 하면 여기서는 MOVE,Up 을 처리하지 않는다.
        when(ev.actionMasked){
            MotionEvent.ACTION_DOWN->{
                maxVelocity=0F
                //속도 트랙커 초기화
                mVelocityTracker?.clear()
                //속도 트랙커가 null 이면 생성.
                mVelocityTracker=mVelocityTracker?: VelocityTracker.obtain()

                //... 중략
                //.
                //.
                //.

            }
            MotionEvent.ACTION_MOVE->{
                //... 중략
                //.
                //.
			

                //

                    if(dy>=touchSlop){

                        isSliding=true

                        //캡쳐 할 수 있게 강제 ACTION_DOWN event 설정.
                        val forcedEvent=MotionEvent.obtain(ev)
                        forcedEvent.action=MotionEvent.ACTION_DOWN
                        mViewDragHelper.processTouchEvent(forcedEvent)
                        return true
                    } else{
                        isSliding=false
                        return false
                    }
                }else{
                    //슬라이딩 중이 아니면(어차피 isSliding 은 false)
                    return isSliding
                }
            }
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{



                isSliding=false
                isSlideViewTouched=false
                isScrollViewTouched=false
                //mViewDragHelper.shouldInterceptTouchEvent(ev)
                return mViewDragHelper.shouldInterceptTouchEvent(ev)




            }


            else -> {
                return mViewDragHelper.shouldInterceptTouchEvent(ev)
            }
        }

    }
```

<br/>

xml 에서 app:moony_SlideLayout_exposureHeight 의 옵션을 사용하여 기본적으로 노출 되어있는 SlideView의 높이를 지정할 수 있습니다.

<br/>

또한 app:moony_SlideLayout_maxHeight의 옵션을 사용하여, 잡아 끌어 당겨서 최대로 올릴 수 있는 높이 또한 지정할 수 있습니다.

<br/>

<br/>

(200dp 일 때)

```xml
<moony.SlideLayout
    android:id="@+id/test_slide_layout"
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:moony_SlideLayout_exposureHeight="200dp"
    app:moony_SlideLayout_maxHeight="600dp"
    >
```

![화면 캡처 2022-03-17 215312](https://user-images.githubusercontent.com/53536205/158812808-3f0a2e74-db0e-43d6-a966-7c268ed4b015.png)



<br/>

<br/>

(500dp 일 때)

```xml
<moony.SlideLayout
    android:id="@+id/test_slide_layout"
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:moony_SlideLayout_exposureHeight="500dp"
    app:moony_SlideLayout_maxHeight="800dp"
    >
```

![화면 캡처 2022-03-17 215348](https://user-images.githubusercontent.com/53536205/158813127-99cb858d-fbbb-4f20-9965-c845d7359463.png)





<br/>

<br/>

## 기능2) 애니메이션

<br/>

사용자가 지정한 크기의 절반 만큼을 Threshold로 설정하여, 접히는지 아닌 지를 판정할 수 있습니다.

<br/>

판정 후에는 ViewDragHelper의 Animation을 사용하여 자동으로 지점까지 움직입니다.

<br/>

![Animation](https://user-images.githubusercontent.com/53536205/158811021-0db1c31a-6e1b-49bd-8ada-918b72db7a40.gif)



<br/>

<br/>

또한 사용자가 뷰를 잡아 끌기 시작하는 때, 혹은 접히거나 펼쳐지는 애니메이션이 끝난 후,

이 두 시점에 **람다**를 이용하여 Callback을 받을 수 있습니다.

```kotlin
binding.testSlideLayout.setOnSlideViewCaptured {  }
binding.testSlideLayout.setOnSlideViewReleased {  }
```



<br/>

<br/>

## 기능3) 스크롤 뷰 등록

<br/>

스크롤 뷰를 등록하면, 등록된 뷰를 터치하는 동안에는 **slideLayout이 움직이는데 영향을 미치지 않습니다.** 

<br/>

하지만, ScrollView가 끝까지(맨 처음의 목록까지) 올라갔을 경우, **SrollView를 슬라이드 하면 slideLayout이 움직입니다.**

<br/>

![Scroll](https://user-images.githubusercontent.com/53536205/158814764-228377b9-905e-4057-a32a-0ca925197be1.gif)

<br/>

<br/>

scrollView는 app:moony_SlideLayout_scrollViewId 옵션을 이용하여 등록할 수 있습니다.

```xml
<moony.SlideLayout
    android:id="@+id/test_slide_layout"
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:moony_SlideLayout_exposureHeight="500dp"
    app:moony_SlideLayout_maxHeight="800dp"
    app:moony_SlideLayout_scrollViewId="@id/test_list_view"
    >
```

<br/>

<br/>

만약 생명주기에 맞춰야 할 경우, 객체의 메소드를 이용해서도 scrollView를 등록할 수 있습니다.

```kotlin
binding.testSlideLayout.setScrollView(R.id.test_list_view)
```



<br/>

**전체 소스코드는 밑의 링크에서 확인할 수 있습니다.**

[https://github.com/Moony-H/SlideUpLayout/blob/master/app/SlideUpLayout/src/main/java/com/example/slideuplayout/SlideUpLayout.kt](https://github.com/Moony-H/SlideUpLayout/blob/master/app/SlideUpLayout/src/main/java/com/example/slideuplayout/SlideUpLayout.kt)

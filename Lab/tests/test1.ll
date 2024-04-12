; ModuleID = 'module'
source_filename = "module"

@sort_arr = global [5 x i32] zeroinitializer

define void @combine(i32* %0, i32 %1, i32* %2, i32 %3) {
combineEntry:
  %pointerToarr1 = alloca i32*, align 8
  store i32* %0, i32** %pointerToarr1, align 8
  %pointerToarr1_length = alloca i32, align 4
  store i32 %1, i32* %pointerToarr1_length, align 4
  %pointerToarr2 = alloca i32*, align 8
  store i32* %2, i32** %pointerToarr2, align 8
  %pointerToarr2_length = alloca i32, align 4
  store i32 %3, i32* %pointerToarr2_length, align 4
  ret void
  ret void
}

define i32 @main() {
mainEntry:
  %a = alloca [2 x i32], align 4
  %pointerToElement_0 = getelementptr [2 x i32], [2 x i32]* %a, i32 0, i32 0
  store i32 1, i32* %pointerToElement_0, align 4
  %pointerToElement_1 = getelementptr [2 x i32], [2 x i32]* %a, i32 0, i32 1
  store i32 5, i32* %pointerToElement_1, align 4
  %b = alloca [3 x i32], align 4
  %pointerToElement_01 = getelementptr [3 x i32], [3 x i32]* %b, i32 0, i32 0
  store i32 1, i32* %pointerToElement_01, align 4
  %pointerToElement_12 = getelementptr [3 x i32], [3 x i32]* %b, i32 0, i32 1
  store i32 4, i32* %pointerToElement_12, align 4
  %pointerToElement_2 = getelementptr [3 x i32], [3 x i32]* %b, i32 0, i32 2
  store i32 14, i32* %pointerToElement_2, align 4
  ret i32 5
  ret i32 0
}

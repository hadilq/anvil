package com.squareup.hephaestus.compiler

import com.google.common.truth.Truth.assertThat
import com.squareup.hephaestus.annotations.MergeComponent
import com.squareup.hephaestus.annotations.MergeSubcomponent
import com.squareup.hephaestus.annotations.compat.MergeInterfaces
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.Result
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.reflect.KClass

@RunWith(Parameterized::class)
class InterfaceMergerTest(
  private val annotationClass: KClass<*>,
  private val skipAnalysis: Boolean
) {

  private val annotation = "@${annotationClass.simpleName}"
  private val import = "import ${annotationClass.java.canonicalName}"

  companion object {
    @Parameters(name = "{0}, skipAnalysis: {1}")
    @JvmStatic fun annotationClasses(): Collection<Array<Any>> {
      return listOf(MergeComponent::class, MergeSubcomponent::class, MergeInterfaces::class)
          .flatMap { clazz ->
            listOf(true, false).map { arrayOf(clazz, it) }
          }
    }
  }

  @Test fun `interfaces are merged successfully`() {
    compile(
        """
        package com.squareup.test
        
        import com.squareup.hephaestus.annotations.ContributesTo
        $import
        
        @ContributesTo(Any::class)
        interface ContributingInterface
        
        @ContributesTo(Any::class)
        interface SecondContributingInterface
        
        $annotation(Any::class)
        interface ComponentInterface
    """
    ) {
      assertThat(componentInterface extends contributingInterface).isTrue()
      assertThat(componentInterface extends secondContributingInterface).isTrue()
    }
  }

  @Test fun `parent interface is merged`() {
    compile(
        """
        package com.squareup.test
        
        import com.squareup.hephaestus.annotations.ContributesTo
        $import
        
        interface ParentInterface
        
        @ContributesTo(Any::class)
        interface ContributingInterface : ParentInterface
        
        $annotation(Any::class)
        interface ComponentInterface
    """
    ) {
      assertThat(componentInterface extends parentInterface).isTrue()
    }
  }

  @Test fun `interfaces are not merged without @Merge annotation`() {
    compile(
        """
        package com.squareup.test
        
        import com.squareup.hephaestus.annotations.ContributesTo
        $import
        
        @ContributesTo(Any::class)
        interface ContributingInterface
        
        interface ComponentInterface
    """
    ) {
      assertThat(componentInterface extends contributingInterface).isFalse()
    }
  }

  @Test fun `interfaces are not merged without @ContributesTo annotation`() {
    compile(
        """
        package com.squareup.test
        
        import com.squareup.hephaestus.annotations.ContributesTo
        $import
        
        interface ContributingInterface
        
        $annotation(Any::class)
        interface ComponentInterface
    """
    ) {
      assertThat(componentInterface extends contributingInterface).isFalse()
    }
  }

  @Test fun `code must be in com_squareup package`() {
    compile(
        """
        package com.other
        
        import com.squareup.hephaestus.annotations.ContributesTo
        $import
        
        @ContributesTo(Any::class)
        interface ContributingInterface
        
        $annotation(Any::class)
        interface ComponentInterface
    """
    ) {
      assertThat(
          classLoader.loadClass("com.other.ComponentInterface") extends
              classLoader.loadClass("com.other.ContributingInterface")
      ).isFalse()
    }
  }

  @Test fun `classes annotated with @MergeComponent must be interfaces`() {
    compile(
        """
        package com.squareup.test
        
        $import
        
        $annotation(Any::class)
        abstract class MergingClass
    """
    ) {
      assertThat(exitCode).isEqualTo(COMPILATION_ERROR)
      // Position to the class.
      assertThat(messages).contains("Source.kt: (6, 16)")
    }
  }

  @Test fun `a contributed interface can be replaced`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo
        $import
        
        @ContributesTo(Any::class)
        interface ContributingInterface
        
        @ContributesTo(
            Any::class,
            replaces = ContributingInterface::class
        )
        interface SecondContributingInterface        

        $annotation(Any::class)
        interface ComponentInterface
    """
    ) {
      assertThat(componentInterface extends contributingInterface).isFalse()
      assertThat(componentInterface extends secondContributingInterface).isTrue()
    }
  }

  @Test fun `replaced interfaces must be interfaces and not classes`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo
        $import
        
        @ContributesTo(Any::class)
        class ContributingInterface
        
        @ContributesTo(
            Any::class,
            replaces = ContributingInterface::class
        )
        interface SecondContributingInterface        

        $annotation(Any::class)
        interface ComponentInterface
    """
    ) {
      assertThat(exitCode).isEqualTo(COMPILATION_ERROR)
      // Position to the class.
      assertThat(messages).contains("Source.kt: (13, 11)")
    }
  }

  @Test fun `predefined interfaces are not replaced`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo
        $import

        @ContributesTo(Any::class)
        interface ContributingInterface
        
        @ContributesTo(
            Any::class,
            replaces = ContributingInterface::class
        )
        interface SecondContributingInterface

        $annotation(Any::class)
        interface ComponentInterface : ContributingInterface
    """
    ) {
      assertThat(componentInterface extends contributingInterface).isTrue()
      assertThat(componentInterface extends secondContributingInterface).isTrue()
    }
  }

  @Test fun `interface can be excluded excluded`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo
        $import

        @ContributesTo(Any::class)
        interface ContributingInterface
        
        @ContributesTo(Any::class)
        interface SecondContributingInterface

        $annotation(
            scope = Any::class,
            exclude = [
              ContributingInterface::class
            ]
        )
        interface ComponentInterface
    """
    ) {
      assertThat(componentInterface extends contributingInterface).isFalse()
      assertThat(componentInterface extends secondContributingInterface).isTrue()
    }
  }

  @Test fun `predefined interfaces is not excluded`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo
        $import

        @ContributesTo(Any::class)
        interface ContributingInterface
        
        @ContributesTo(Any::class)
        interface SecondContributingInterface

        $annotation(
            scope = Any::class,
            exclude = [
              ContributingInterface::class
            ]
        )
        interface ComponentInterface : ContributingInterface
    """
    ) {
      assertThat(componentInterface extends contributingInterface).isTrue()
      assertThat(componentInterface extends secondContributingInterface).isTrue()
    }
  }

  @Test fun `interfaces are added to components with corresponding scope`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo
        $import

        @ContributesTo(Any::class)
        interface ContributingInterface
        
        @ContributesTo(Unit::class)
        interface SecondContributingInterface

        $annotation(Any::class)
        interface ComponentInterface
        
        $annotation(Unit::class)
        interface SubcomponentInterface
    """
    ) {
      assertThat(componentInterface extends contributingInterface).isTrue()
      assertThat(componentInterface extends secondContributingInterface).isFalse()

      assertThat(subcomponentInterface extends contributingInterface).isFalse()
      assertThat(subcomponentInterface extends secondContributingInterface).isTrue()
    }
  }

  @Test fun `interfaces are added to components with corresponding scope and component type`() {
    assumeMergeComponent(annotationClass)

    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo
        import com.squareup.hephaestus.annotations.MergeComponent
        import com.squareup.hephaestus.annotations.MergeSubcomponent

        @ContributesTo(Any::class)
        interface ContributingInterface
        
        @ContributesTo(Unit::class)
        interface SecondContributingInterface

        @MergeComponent(Any::class)
        interface ComponentInterface
        
        @MergeSubcomponent(Unit::class)
        interface SubcomponentInterface
    """
    ) {
      assertThat(componentInterface extends contributingInterface).isTrue()
      assertThat(componentInterface extends secondContributingInterface).isFalse()

      assertThat(subcomponentInterface extends contributingInterface).isFalse()
      assertThat(subcomponentInterface extends secondContributingInterface).isTrue()
    }
  }

  @Test fun `contributed interfaces must be public`() {
    val visibilities = setOf(
        "internal", "private", "protected"
    )

    visibilities.forEach { visibility ->
      compile(
          """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo
        $import

        @ContributesTo(Any::class)
        $visibility interface ContributingInterface
        
        $annotation(Any::class)
        interface ComponentInterface
    """
      ) {
        assertThat(exitCode).isEqualTo(COMPILATION_ERROR)
        // Position to the class.
        assertThat(messages).contains("Source.kt: (7, ")
      }
    }
  }

  @Test fun `inner interfaces are merged`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo
        $import

        class SomeClass {
          @ContributesTo(Any::class)
          interface InnerInterface
        }
        
        $annotation(Any::class)
        interface ComponentInterface
    """
    ) {
      assertThat(componentInterface extends innerInterface).isTrue()
    }
  }

  @Test fun `inner interfaces in merged component are not merged`() {
    // They could cause errors while compiling code when adding our contributed super classes.
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo
        $import
        
        $annotation(Any::class)
        interface ComponentInterface {
          @ContributesTo(Any::class)
          interface InnerInterface
        }
    """
    ) {
      assertThat(
          componentInterface extends
              classLoader.loadClass("com.squareup.test.ComponentInterface\$InnerInterface")
      ).isFalse()
    }
  }

  private fun compile(
    source: String,
    block: Result.() -> Unit = { }
  ): Result = compile(source, skipAnalysis, block = block)
}
